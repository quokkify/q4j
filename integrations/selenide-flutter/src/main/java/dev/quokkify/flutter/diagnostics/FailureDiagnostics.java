package dev.quokkify.flutter.diagnostics;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.quokkify.flutter.locator.LocatorSpec;
import dev.quokkify.flutter.locator.Locators;
import dev.quokkify.flutter.suggest.NearMiss;
import dev.quokkify.flutter.suggest.NearMissFinder;
import dev.quokkify.flutter.tree.WidgetNode;
import dev.quokkify.flutter.tree.WidgetTreeFormatter;
import dev.quokkify.flutter.tree.WidgetTreeParser;
import dev.quokkify.util.JsonConverter;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

public final class FailureDiagnostics {

  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
  public static final int DEFAULT_MAX_CHARS = 2_000_000;

  private static final String RENDER_TREE = "flutter: renderTree";
  private static final String TRUNCATED_MARKER = "\n…[truncated]";
  private static final String THREAD_NAME = "flutter-render-tree";
  // java-client parses responses with a JSON nesting limit (255) that a whole app tree exceeds; the Scaffold
  // subtrees are shallow enough, and the last one belongs to the topmost route.
  private static final String NESTING_LIMIT = "Nesting limit";
  private static final Map<String, Object> SCAFFOLDS = Map.of("widgetType", "Scaffold");

  private final Duration timeout;
  private final int maxChars;

  public FailureDiagnostics(Duration timeout, int maxChars) {
    if (timeout == null || timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("timeout must be positive but was " + timeout);
    }
    if (maxChars <= 0) {
      throw new IllegalArgumentException("maxChars must be positive but was " + maxChars);
    }
    this.timeout = timeout;
    this.maxChars = maxChars;
  }

  public Diagnostics collect(JavascriptExecutor driver, By failed) {
    LocatorSpec spec = Locators.parse(failed);
    if (!spec.strategy().isFlutter()) {
      return new Diagnostics(null, null, List.of(), null, false);
    }
    try {
      return diagnose(spec, renderTree(driver));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return failure(e);
    } catch (ExecutionException e) {
      return failure(e.getCause() == null ? e : e.getCause());
    } catch (TimeoutException | RuntimeException | StackOverflowError e) {
      return failure(e);
    }
  }

  private Diagnostics diagnose(LocatorSpec spec, Object result) {
    String json = result instanceof String text ? text : JsonConverter.toJson(result);
    boolean truncated = json.length() > maxChars;
    String treeJson = truncated ? truncate(json) : json;
    try {
      WidgetNode root = WidgetTreeParser.fromScriptResult(result);
      List<NearMiss> suggestions = NearMissFinder.find(spec, root, NearMissFinder.DEFAULT_LIMIT);
      String text = WidgetTreeFormatter.format(root);
      return new Diagnostics(treeJson, text.length() > maxChars ? truncate(text) : text, suggestions, null, truncated);
    } catch (RuntimeException | StackOverflowError e) {
      return new Diagnostics(treeJson, null, List.of(), describe(e), truncated);
    }
  }

  private Object renderTree(JavascriptExecutor driver)
      throws InterruptedException, ExecutionException, TimeoutException {
    ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
      Thread thread = new Thread(task, THREAD_NAME);
      thread.setDaemon(true);
      return thread;
    });
    try {
      Future<Object> future = executor.submit(() -> fetchTree(driver));
      try {
        return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        future.cancel(true);
        throw new TimeoutException(RENDER_TREE + " did not finish within " + timeout.toMillis() + " ms");
      }
    } finally {
      executor.shutdownNow();
    }
  }

  private static Object fetchTree(JavascriptExecutor driver) {
    try {
      return driver.executeScript(RENDER_TREE, Map.of());
    } catch (RuntimeException e) {
      if (!isTooDeep(e)) {
        throw e;
      }
      Object scaffolds = driver.executeScript(RENDER_TREE, SCAFFOLDS);
      return scaffolds instanceof List<?> list && !list.isEmpty() ? list.get(list.size() - 1) : scaffolds;
    }
  }

  private static boolean isTooDeep(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause.getMessage() != null && cause.getMessage().contains(NESTING_LIMIT)) {
        return true;
      }
    }
    return false;
  }

  private String truncate(String value) {
    int end = Character.isHighSurrogate(value.charAt(maxChars - 1)) ? maxChars - 1 : maxChars;
    return value.substring(0, end) + TRUNCATED_MARKER;
  }

  private static Diagnostics failure(Throwable error) {
    return new Diagnostics(null, null, List.of(), describe(error), false);
  }

  private static String describe(Throwable error) {
    return error.getClass().getSimpleName() + ": " + error.getMessage();
  }
}
