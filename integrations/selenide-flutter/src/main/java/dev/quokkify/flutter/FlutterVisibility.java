package dev.quokkify.flutter;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import dev.quokkify.flutter.diagnostics.Diagnostics;
import dev.quokkify.flutter.diagnostics.FailureDiagnostics;
import dev.quokkify.flutter.report.AllureSink;
import dev.quokkify.flutter.report.DiagnosticsSink;
import dev.quokkify.flutter.report.LogSink;
import dev.quokkify.flutter.suggest.NearMiss;
import dev.quokkify.flutter.trace.AttemptSummary;
import dev.quokkify.flutter.trace.FindAttempt;
import dev.quokkify.flutter.trace.FindTracer;
import dev.quokkify.flutter.tree.WidgetTreeFormatter;

import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.LogEventListener;
import com.codeborne.selenide.logevents.SelenideLogger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlutterVisibility {

  static final String LISTENER_NAME = "FlutterVisibility";
  static final String SEARCH = LogSink.SUMMARY_NAME;
  public static final String TREE_JSON = "Flutter: widget tree (json)";
  static final String TREE = "Flutter: widget tree";
  static final String SUGGESTIONS = "Flutter: suggestions";
  static final String ERROR = "Flutter: diagnostics error";

  private static final String TEXT = "text/plain";
  private static final String JSON = "application/json";
  private static final Logger LOG = LoggerFactory.getLogger(FlutterVisibility.class);
  private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);
  private static final FindTracer TRACER = new FindTracer(Clock.systemUTC(), ACTIVE::get);
  private static final Object LOCK = new Object();

  static volatile Supplier<Optional<WebDriver>> currentDriver = FlutterVisibility::startedDriver;
  static volatile Consumer<WebDriverListener> addDriverListener = WebDriverRunner::addListener;
  static volatile Consumer<WebDriverListener> removeDriverListener = WebDriverRunner::removeListener;

  private static boolean driverListenerRegistered;

  private FlutterVisibility() {
  }

  /**
   * Installs Flutter visibility with Allure and log reporting for the calling thread.
   *
   * @see #install(FailureDiagnostics, List)
   */
  public static void install() {
    install(new FailureDiagnostics(FailureDiagnostics.DEFAULT_TIMEOUT, FailureDiagnostics.DEFAULT_MAX_CHARS),
        List.of(new AllureSink(), new LogSink()));
  }

  /**
   * Installs Flutter visibility for the calling thread. Selenide log listeners are per thread, so call this on
   * each test thread (for example in {@code @BeforeMethod}) before that thread creates its first driver; the
   * find tracer only wraps drivers created after its one global registration. Repeated calls on a thread are
   * idempotent and the latest configuration wins.
   */
  public static void install(FailureDiagnostics diagnostics, List<DiagnosticsSink> sinks) {
    synchronized (LOCK) {
      if (!driverListenerRegistered) {
        addDriverListener.accept(TRACER);
        driverListenerRegistered = true;
        warnIfDriverAlreadyStarted();
      }
    }
    if (SelenideLogger.hasListener(LISTENER_NAME)) {
      LOG.debug("FlutterVisibility already installed on this thread; replacing its configuration");
    }
    SelenideLogger.addListener(LISTENER_NAME, new Listener(diagnostics, List.copyOf(sinks)));
    ACTIVE.set(Boolean.TRUE);
  }

  /**
   * Removes Flutter visibility from the calling thread. The global find tracer stays registered with
   * {@link WebDriverRunner} but records nothing on threads where Flutter visibility is not installed.
   */
  public static void uninstall() {
    SelenideLogger.removeListener(LISTENER_NAME);
    ACTIVE.remove();
    TRACER.clear();
    TRACER.resetTotal();
  }

  public static FindTracer tracer() {
    return TRACER;
  }

  static void reset() {
    uninstall();
    synchronized (LOCK) {
      if (driverListenerRegistered) {
        removeDriverListener.accept(TRACER);
        driverListenerRegistered = false;
      }
    }
  }

  private static void warnIfDriverAlreadyStarted() {
    try {
      if (currentDriver.get().isPresent()) {
        LOG.warn("FlutterVisibility installed after the WebDriver started; find attempts of that driver are not"
            + " traced. Call FlutterVisibility.install() before the first driver is created.");
      }
    } catch (RuntimeException | StackOverflowError e) {
      LOG.debug("Could not check whether a WebDriver already started", e);
    }
  }

  private static Optional<WebDriver> startedDriver() {
    return WebDriverRunner.hasWebDriverStarted() ? Optional.of(WebDriverRunner.getWebDriver()) : Optional.empty();
  }

  private static String suggestionLine(NearMiss miss) {
    return String.format(Locale.ROOT, "%.2f", miss.score()) + " " + WidgetTreeFormatter.line(miss.node()) + " — "
        + String.join("; ", miss.reasons());
  }

  private static String describe(Throwable error) {
    return error.getClass().getSimpleName() + ": " + error.getMessage();
  }

  private record Listener(FailureDiagnostics diagnostics, List<DiagnosticsSink> sinks) implements LogEventListener {

    @Override
    public void beforeEvent(LogEvent event) {
      TRACER.clear();
    }

    @Override
    public void afterEvent(LogEvent event) {
      try {
        report(event);
      } catch (RuntimeException | StackOverflowError e) {
        LOG.warn("Flutter visibility reporting failed", e);
      }
    }

    private void report(LogEvent event) {
      List<FindAttempt> attempts = TRACER.drain();
      if (attempts.isEmpty()) {
        return;
      }
      attach(SEARCH, TEXT, AttemptSummary.of(attempts).render());
      if (event.getStatus() != LogEvent.EventStatus.FAIL) {
        return;
      }
      try {
        Optional<WebDriver> driver = currentDriver.get();
        if (driver.isPresent() && driver.get() instanceof JavascriptExecutor js) {
          attachDiagnostics(diagnostics.collect(js, attempts.get(attempts.size() - 1).by()));
        }
      } catch (RuntimeException | StackOverflowError e) {
        LOG.warn("Flutter failure diagnostics could not be collected", e);
        attach(ERROR, TEXT, describe(e));
      }
    }

    private void attachDiagnostics(Diagnostics d) {
      if (d.treeJson() != null) {
        attach(TREE_JSON, d.truncated() ? TEXT : JSON, d.treeJson());
      }
      if (d.treeText() != null) {
        attach(TREE, TEXT, d.treeText());
      }
      if (!d.suggestions().isEmpty()) {
        attach(SUGGESTIONS, TEXT, d.suggestions().stream().map(FlutterVisibility::suggestionLine)
            .collect(Collectors.joining(System.lineSeparator())));
      }
      if (d.error() != null) {
        attach(ERROR, TEXT, d.error());
      }
    }

    private void attach(String name, String mimeType, String content) {
      for (DiagnosticsSink sink : sinks) {
        try {
          sink.attach(name, mimeType, content);
        } catch (RuntimeException e) {
          LOG.warn("Diagnostics sink {} failed to attach '{}'", sink.getClass().getName(), name, e);
        }
      }
    }
  }
}
