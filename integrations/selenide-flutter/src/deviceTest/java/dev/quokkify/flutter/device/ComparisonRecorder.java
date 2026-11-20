package dev.quokkify.flutter.device;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import dev.quokkify.flutter.FlutterVisibility;
import dev.quokkify.flutter.comparison.ComparisonReport;
import dev.quokkify.flutter.comparison.NativeExcerpt;
import dev.quokkify.flutter.comparison.ScenarioResult;
import dev.quokkify.flutter.comparison.Status;
import dev.quokkify.flutter.report.DiagnosticsSink;
import dev.quokkify.util.JsonConverter;

import com.codeborne.selenide.WebDriverRunner;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.model.TestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Records one {@link ScenarioResult} per device test and, when the suite finishes, writes
 * {@code build/flutter-comparison/{summary.md,results.json,verdict.txt}} and attaches the summary to Allure.
 *
 * <p>Find attempts come from the per-thread {@link FlutterVisibility#tracer()} total, which is reset when a test
 * starts and read right after the test method returns (the {@code @AfterMethod} of {@link BaseMobileTest} resets it
 * again). The failure demo's diagnostics come from the {@code Flutter: suggestions} attachment in flutter mode,
 * captured through {@link #sink()}, and from the native page source in native mode.
 */
public class ComparisonRecorder implements ITestListener, ISuiteListener, IInvokedMethodListener {

  static final String OUTPUT_DIR = "build/flutter-comparison";
  private static final String SUGGESTIONS = "Flutter: suggestions";
  private static final String ATTACHMENT_NAME = "Flutter vs native comparison";
  private static final String NOT_REACHABLE_PREFIX = "not reachable in ";
  private static final int EXCERPT_LINES = 15;
  private static final Logger LOG = LoggerFactory.getLogger(ComparisonRecorder.class);

  private static final ThreadLocal<String> SUGGESTIONS_TEXT = new ThreadLocal<>();
  private static final ThreadLocal<Long> FINDS = ThreadLocal.withInitial(() -> 0L);

  private final Queue<ScenarioResult> results = new ConcurrentLinkedQueue<>();

  /**
   * A diagnostics sink that remembers the Flutter suggestions of the calling thread's current test.
   */
  public static DiagnosticsSink sink() {
    return (name, mimeType, content) -> {
      if (SUGGESTIONS.equals(name)) {
        SUGGESTIONS_TEXT.set(content);
      }
    };
  }

  @Override
  public void onTestStart(ITestResult result) {
    SUGGESTIONS_TEXT.remove();
    FlutterVisibility.tracer().resetTotal();
    FINDS.set(0L);
  }

  @Override
  public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
    if (method.isTestMethod()) {
      FINDS.set(FlutterVisibility.tracer().totalAttempts());
    }
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    record(result, Status.PASSED);
  }

  @Override
  public void onTestFailure(ITestResult result) {
    record(result, isKnownGap(result) ? Status.KNOWN_GAP : Status.FAILED);
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    Throwable error = result.getThrowable();
    boolean notReachable = error != null && error.getMessage() != null
        && result.getInstance() instanceof BaseMobileTest test
        && error.getMessage().startsWith(NOT_REACHABLE_PREFIX + test.mode.label() + " mode:");
    record(result, notReachable ? Status.NOT_REACHABLE : Status.FAILED);
  }

  @Override
  public void onFinish(ISuite suite) {
    List<ScenarioResult> all = List.copyOf(results);
    String summary = ComparisonReport.render(all);
    String verdict = ComparisonReport.verdict(all).orElse(ComparisonReport.OK);
    try {
      Path dir = Files.createDirectories(Path.of(OUTPUT_DIR));
      Files.writeString(dir.resolve("summary.md"), summary, StandardCharsets.UTF_8);
      Files.writeString(dir.resolve("results.json"), JsonConverter.toJson(all), StandardCharsets.UTF_8);
      Files.writeString(dir.resolve("verdict.txt"), verdict, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write the comparison summary", e);
    }
    attachToAllure(summary, ComparisonReport.OK.equals(verdict));
  }

  private void record(ITestResult result, Status status) {
    if (!(result.getInstance() instanceof BaseMobileTest test)) {
      return;
    }
    String scenario = result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
    boolean demo = result.getTestClass().getRealClass() == FailureDemoTest.class;
    String gapReason = isKnownGap(result) ? result.getMethod().getDescription() : null;
    results.add(new ScenarioResult(scenario, test.mode.label(), status,
        result.getEndMillis() - result.getStartMillis(), FINDS.get(), demo,
        demo && status == Status.FAILED ? excerpt(test.mode) : null, gapReason));
  }

  private static boolean isKnownGap(ITestResult result) {
    return result.getInstance() instanceof BaseMobileTest test
        && Arrays.asList(result.getMethod().getGroups()).contains(KnownGapInterceptor.GROUP_PREFIX + test.mode.label());
  }

  private static String excerpt(MobileMode mode) {
    if (mode == MobileMode.NATIVE) {
      String source = nativePageSource();
      return source == null ? null : NativeExcerpt.of(source, EXCERPT_LINES);
    }
    String text = SUGGESTIONS_TEXT.get();
    return text == null ? null : text.lines().limit(EXCERPT_LINES).collect(Collectors.joining("\n"));
  }

  private static String nativePageSource() {
    try {
      return WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver().getPageSource() : null;
    } catch (RuntimeException e) {
      LOG.warn("Could not read the native page source for the failure demo", e);
      return null;
    }
  }

  private static void attachToAllure(String summary, boolean ok) {
    AllureLifecycle lifecycle = Allure.getLifecycle();
    AllureExternalKey key = AllureExternalKey.random(ComparisonRecorder.class);
    lifecycle.scheduleTest(key, new TestResult().setName(ATTACHMENT_NAME)
        .setFullName(ComparisonRecorder.class.getName()));
    lifecycle.startTest(key);
    lifecycle.addAttachment(key, ATTACHMENT_NAME, "text/markdown",
        new ByteArrayInputStream(summary.getBytes(StandardCharsets.UTF_8)), AttachmentOptions.withFileExtension(".md"));
    lifecycle.updateTest(result -> result.setStatus(ok ? io.qameta.allure.model.Status.PASSED
        : io.qameta.allure.model.Status.FAILED));
    lifecycle.stopTest(key);
    lifecycle.writeTest(key);
  }
}
