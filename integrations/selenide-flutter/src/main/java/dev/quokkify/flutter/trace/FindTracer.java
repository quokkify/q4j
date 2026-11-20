package dev.quokkify.flutter.trace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;

import dev.quokkify.flutter.locator.Locators;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverListener;

public final class FindTracer implements WebDriverListener {

  private static final String FIND_ELEMENT = "findElement";
  private static final String FIND_ELEMENTS = "findElements";
  // The Appium Flutter server reports a miss as an unknown error (HTTP 500), not as "no such element".
  private static final String FLUTTER_SERVER_MISS = "is not present in DOM";

  private final Clock clock;
  private final BooleanSupplier recording;
  private final ThreadLocal<Deque<Pending>> pending = ThreadLocal.withInitial(ArrayDeque::new);
  private final ThreadLocal<List<FindAttempt>> finished = ThreadLocal.withInitial(ArrayList::new);
  private final ThreadLocal<long[]> total = ThreadLocal.withInitial(() -> new long[1]);

  public FindTracer(Clock clock, BooleanSupplier recording) {
    this.clock = clock;
    this.recording = recording;
  }

  public FindTracer(Clock clock) {
    this(clock, () -> true);
  }

  public FindTracer() {
    this(Clock.systemUTC());
  }

  public List<FindAttempt> drain() {
    List<FindAttempt> attempts = finished.get();
    List<FindAttempt> copy = new ArrayList<>(attempts);
    attempts.clear();
    return copy;
  }

  public void clear() {
    pending.remove();
    finished.remove();
  }

  public long totalAttempts() {
    return total.get()[0];
  }

  public void resetTotal() {
    total.remove();
  }

  @Override
  public void beforeFindElement(WebDriver driver, By locator) {
    start(locator);
  }

  @Override
  public void afterFindElement(WebDriver driver, By locator, WebElement result) {
    finish(locator, Outcome.FOUND, null);
  }

  @Override
  public void beforeFindElement(WebElement element, By locator) {
    start(locator);
  }

  @Override
  public void afterFindElement(WebElement element, By locator, WebElement result) {
    finish(locator, Outcome.FOUND, null);
  }

  @Override
  public void beforeFindElements(WebDriver driver, By locator) {
    start(locator);
  }

  @Override
  public void afterFindElements(WebDriver driver, By locator, List<WebElement> result) {
    finish(locator, result.isEmpty() ? Outcome.NOT_FOUND : Outcome.FOUND, null);
  }

  @Override
  public void beforeFindElements(WebElement element, By locator) {
    start(locator);
  }

  @Override
  public void afterFindElements(WebElement element, By locator, List<WebElement> result) {
    finish(locator, result.isEmpty() ? Outcome.NOT_FOUND : Outcome.FOUND, null);
  }

  @Override
  public void onError(Object target, Method method, Object[] args, InvocationTargetException e) {
    String name = method.getName();
    if (!FIND_ELEMENT.equals(name) && !FIND_ELEMENTS.equals(name)) {
      return;
    }
    if (args == null || args.length == 0 || !(args[0] instanceof By locator)) {
      return;
    }
    Throwable cause = e.getCause() == null ? e : e.getCause();
    if (cause instanceof NoSuchElementException || isFlutterServerMiss(cause)) {
      finish(locator, Outcome.NOT_FOUND, null);
    } else {
      finish(locator, Outcome.ERROR, firstLine(cause));
    }
  }

  private static boolean isFlutterServerMiss(Throwable cause) {
    String message = cause.getMessage();
    return message != null && message.contains(FLUTTER_SERVER_MISS);
  }

  private void start(By locator) {
    if (!recording.getAsBoolean()) {
      return;
    }
    pending.get().push(new Pending(locator, clock.instant()));
  }

  private void finish(By locator, Outcome outcome, String detail) {
    Pending match = takePending(locator);
    if (match == null) {
      return;
    }
    Instant end = clock.instant();
    finished.get().add(new FindAttempt(locator, Locators.describe(locator), match.start(),
        Duration.between(match.start(), end), outcome, detail));
    total.get()[0]++;
  }

  private Pending takePending(By locator) {
    Iterator<Pending> it = pending.get().iterator();
    while (it.hasNext()) {
      Pending candidate = it.next();
      if (candidate.by().equals(locator)) {
        it.remove();
        return candidate;
      }
    }
    return null;
  }

  private static String firstLine(Throwable t) {
    String message = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    int newline = message.indexOf('\n');
    return newline < 0 ? message : message.substring(0, newline);
  }

  private record Pending(By by, Instant start) {
  }
}
