package dev.quokkify.flutter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import dev.quokkify.flutter.diagnostics.FailureDiagnostics;
import dev.quokkify.flutter.report.DiagnosticsSink;
import dev.quokkify.flutter.trace.FindTracer;

import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.SelenideLog;
import com.codeborne.selenide.logevents.SelenideLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class FlutterVisibilityTest {

  private static final String SEARCH = "Flutter: element search";
  private static final String TREE_JSON = "Flutter: widget tree (json)";
  private static final String TREE = "Flutter: widget tree";
  private static final String SUGGESTIONS = "Flutter: suggestions";
  private static final String ERROR = "Flutter: diagnostics error";

  private final By failed = FlutterBy.key("login_btn");
  private final FailureDiagnostics diagnostics = new FailureDiagnostics(Duration.ofSeconds(1), 1_000_000);
  private Supplier<Optional<WebDriver>> originalDriver;
  private WebDriver driver;
  private RecordingSink sink;

  @BeforeMethod
  public void setUp() throws Exception {
    originalDriver = FlutterVisibility.currentDriver;
    sink = new RecordingSink();
    driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
    when(((JavascriptExecutor) driver).executeScript("flutter: renderTree", Map.of()))
        .thenReturn(fixture());
  }

  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    FlutterVisibility.uninstall();
    FlutterVisibility.currentDriver = originalDriver;
  }

  @Test
  public void failedCommandAttachesSearchTreeAndSuggestions() throws Exception {
    FlutterVisibility.currentDriver = () -> Optional.of(driver);
    FlutterVisibility.install(diagnostics, List.of(sink));

    failingCommand();

    assertThat(sink.names()).containsExactlyInAnyOrder(SEARCH, TREE_JSON, TREE, SUGGESTIONS);
    assertThat(sink.content(SEARCH)).startsWith("1 find attempt(s)").contains("flutter key \"login_btn\"");
    assertThat(sink.mimeType(TREE_JSON)).isEqualTo("application/json");
    assertThat(sink.mimeType(TREE)).isEqualTo("text/plain");
    assertThat(sink.content(SUGGESTIONS)).contains("ElevatedButton key=\"login_button\"").contains(" — ");
  }

  @Test
  public void noDriverSkipsDiagnostics() throws Exception {
    FlutterVisibility.currentDriver = Optional::empty;
    FlutterVisibility.install(diagnostics, List.of(sink));

    failingCommand();

    assertThat(sink.names()).containsExactly(SEARCH);
  }

  @Test
  public void passedCommandAttachesOnlySearchSummary() throws Exception {
    FlutterVisibility.currentDriver = () -> Optional.of(driver);
    FlutterVisibility.install(diagnostics, List.of(sink));

    SelenideLog log = SelenideLogger.beginStep("$(login_btn)", "should be visible");
    notFound(FlutterVisibility.tracer());
    SelenideLogger.commitStep(log, LogEvent.EventStatus.PASS);

    assertThat(sink.names()).containsExactly(SEARCH);
  }

  @Test
  public void commandWithoutAttemptsAttachesNothing() {
    FlutterVisibility.currentDriver = () -> Optional.of(driver);
    FlutterVisibility.install(diagnostics, List.of(sink));

    SelenideLog log = SelenideLogger.beginStep("open", "app");
    SelenideLogger.commitStep(log, LogEvent.EventStatus.FAIL);

    assertThat(sink.names()).isEmpty();
  }

  @Test
  public void leftoversFromBeforeCommandAreDiscarded() throws Exception {
    FlutterVisibility.currentDriver = Optional::empty;
    FlutterVisibility.install(diagnostics, List.of(sink));
    notFound(FlutterVisibility.tracer());

    failingCommand();

    assertThat(sink.content(SEARCH)).startsWith("1 find attempt(s)");
  }

  @Test
  public void diagnosticsErrorIsAttached() throws Exception {
    when(((JavascriptExecutor) driver).executeScript("flutter: renderTree", Map.of()))
        .thenThrow(new IllegalStateException("no flutter"));
    FlutterVisibility.currentDriver = () -> Optional.of(driver);
    FlutterVisibility.install(diagnostics, List.of(sink));

    failingCommand();

    assertThat(sink.names()).containsExactlyInAnyOrder(SEARCH, ERROR);
    assertThat(sink.content(ERROR)).isEqualTo("IllegalStateException: no flutter");
  }

  @Test
  public void sinkFailureIsNeverThrown() throws Exception {
    FlutterVisibility.currentDriver = () -> Optional.of(driver);
    DiagnosticsSink broken = (name, mimeType, content) -> {
      throw new IllegalStateException("sink down");
    };
    FlutterVisibility.install(diagnostics, List.of(broken, sink));

    assertThatCode(this::failingCommand).doesNotThrowAnyException();
    assertThat(sink.names()).containsExactlyInAnyOrder(SEARCH, TREE_JSON, TREE, SUGGESTIONS);
  }

  @Test
  public void installIsIdempotent() throws Exception {
    FlutterVisibility.currentDriver = () -> Optional.of(driver);
    FlutterVisibility.install(diagnostics, List.of(sink));
    FlutterVisibility.install(diagnostics, List.of(sink));
    assertThat(SelenideLogger.hasListener("FlutterVisibility")).isTrue();

    failingCommand();

    assertThat(sink.names()).containsExactlyInAnyOrder(SEARCH, TREE_JSON, TREE, SUGGESTIONS);
    FlutterVisibility.uninstall();
    assertThat(SelenideLogger.hasListener("FlutterVisibility")).isFalse();
  }

  @Test
  public void defaultInstallRegistersListenerAndTracerIsSingleton() {
    FindTracer tracer = FlutterVisibility.tracer();
    FlutterVisibility.install();
    FlutterVisibility.install();

    assertThat(SelenideLogger.hasListener("FlutterVisibility")).isTrue();
    assertThat(FlutterVisibility.tracer()).isSameAs(tracer);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> fixture() throws IOException {
    try (InputStream in = FlutterVisibilityTest.class.getClassLoader().getResourceAsStream("trees/login-screen.json")) {
      return new ObjectMapper().readValue(in, Map.class);
    }
  }

  private void failingCommand() throws Exception {
    SelenideLog log = SelenideLogger.beginStep("$(login_btn)", "click");
    notFound(FlutterVisibility.tracer());
    SelenideLogger.commitStep(log, new IllegalStateException("element not found"));
  }

  private void notFound(FindTracer tracer) throws Exception {
    Method findElement = WebDriver.class.getMethod("findElement", By.class);
    tracer.beforeFindElement(driver, failed);
    tracer.onError(driver, findElement, new Object[] {failed},
        new InvocationTargetException(new NoSuchElementException("Unable to locate element")));
  }

  private static final class RecordingSink implements DiagnosticsSink {

    private final List<String[]> calls = new ArrayList<>();

    @Override
    public void attach(String name, String mimeType, String content) {
      calls.add(new String[] {name, mimeType, content});
    }

    List<String> names() {
      return calls.stream().map(c -> c[0]).toList();
    }

    String mimeType(String name) {
      return find(name)[1];
    }

    String content(String name) {
      return find(name)[2];
    }

    private String[] find(String name) {
      return calls.stream().filter(c -> c[0].equals(name)).findFirst().orElseThrow();
    }
  }
}
