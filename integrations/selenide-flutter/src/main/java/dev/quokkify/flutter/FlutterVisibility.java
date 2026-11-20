package dev.quokkify.flutter;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlutterVisibility {

  static final String LISTENER_NAME = "FlutterVisibility";
  static final String SEARCH = LogSink.SUMMARY_NAME;
  static final String TREE_JSON = "Flutter: widget tree (json)";
  static final String TREE = "Flutter: widget tree";
  static final String SUGGESTIONS = "Flutter: suggestions";
  static final String ERROR = "Flutter: diagnostics error";

  private static final String TEXT = "text/plain";
  private static final String JSON = "application/json";
  private static final Logger LOG = LoggerFactory.getLogger(FlutterVisibility.class);
  private static final FindTracer TRACER = new FindTracer();

  static volatile Supplier<Optional<WebDriver>> currentDriver = FlutterVisibility::startedDriver;

  private static final Object LOCK = new Object();
  private static boolean installed;

  private FlutterVisibility() {
  }

  public static void install() {
    install(new FailureDiagnostics(FailureDiagnostics.DEFAULT_TIMEOUT, FailureDiagnostics.DEFAULT_MAX_CHARS),
        List.of(new AllureSink(), new LogSink()));
  }

  public static void install(FailureDiagnostics diagnostics, List<DiagnosticsSink> sinks) {
    synchronized (LOCK) {
      if (installed) {
        return;
      }
      SelenideLogger.addListener(LISTENER_NAME, new Listener(diagnostics, List.copyOf(sinks)));
      WebDriverRunner.addListener(TRACER);
      installed = true;
    }
  }

  public static void uninstall() {
    synchronized (LOCK) {
      SelenideLogger.removeListener(LISTENER_NAME);
      WebDriverRunner.removeListener(TRACER);
      TRACER.clear();
      installed = false;
    }
  }

  public static FindTracer tracer() {
    return TRACER;
  }

  private static Optional<WebDriver> startedDriver() {
    return WebDriverRunner.hasWebDriverStarted() ? Optional.of(WebDriverRunner.getWebDriver()) : Optional.empty();
  }

  private record Listener(FailureDiagnostics diagnostics, List<DiagnosticsSink> sinks) implements LogEventListener {

    @Override
    public void beforeEvent(LogEvent event) {
      TRACER.clear();
    }

    @Override
    public void afterEvent(LogEvent event) {
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
          report(diagnostics.collect(js, attempts.get(attempts.size() - 1).by()));
        }
      } catch (RuntimeException e) {
        LOG.warn("Flutter failure diagnostics could not be collected", e);
      }
    }

    private void report(Diagnostics d) {
      if (d.treeJson() != null) {
        attach(TREE_JSON, JSON, d.treeJson());
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

  private static String suggestionLine(NearMiss miss) {
    return String.format(Locale.ROOT, "%.2f", miss.score()) + " " + WidgetTreeFormatter.line(miss.node()) + " — "
        + String.join("; ", miss.reasons());
  }
}
