package dev.quokkify.flutter.trace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import dev.quokkify.flutter.FlutterBy;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FindTracerTest {

  private final By by = FlutterBy.key("delayed_result");
  private final WebDriver driver = mock(WebDriver.class);
  private final WebElement element = mock(WebElement.class);
  private MutableClock clock;
  private FindTracer tracer;
  private Method findElementMethod;

  @BeforeMethod
  public void setUp() throws Exception {
    clock = new MutableClock();
    tracer = new FindTracer(clock);
    findElementMethod = WebDriver.class.getMethod("findElement", By.class);
  }

  @Test
  public void recordsNotFoundThenFound() {
    tracer.beforeFindElement(driver, by);
    clock.advance(40);
    tracer.onError(driver, findElementMethod, new Object[] {by},
        new InvocationTargetException(new NoSuchElementException("Unable to locate element")));
    tracer.beforeFindElement(driver, by);
    clock.advance(30);
    tracer.afterFindElement(driver, by, element);

    List<FindAttempt> attempts = tracer.drain();

    assertThat(attempts).extracting(FindAttempt::outcome).containsExactly(Outcome.NOT_FOUND, Outcome.FOUND);
    assertThat(attempts.get(0).duration()).isEqualTo(Duration.ofMillis(40));
    assertThat(attempts.get(0).locator()).isEqualTo("flutter key \"delayed_result\"");
    assertThat(attempts.get(0).by()).isSameAs(by);
    assertThat(tracer.drain()).isEmpty();
  }

  @Test
  public void flutterServerMissIsNotFound() {
    tracer.beforeFindElement(driver, by);
    tracer.onError(driver, findElementMethod, new Object[] {by},
        new InvocationTargetException(new WebDriverException("Exception: Element with locator widget with key"
            + " [<'login_btn'>] (considering only hit-testable widgets with a RenderBox) is not present in DOM"
            + " with 0 seconds")));

    FindAttempt attempt = tracer.drain().get(0);

    assertThat(attempt.outcome()).isEqualTo(Outcome.NOT_FOUND);
    assertThat(attempt.detail()).isNull();
  }

  @Test
  public void otherErrorIsErrorWithFirstLineAsDetail() {
    tracer.beforeFindElement(driver, by);
    tracer.onError(driver, findElementMethod, new Object[] {by},
        new InvocationTargetException(new IllegalStateException("boom\nsecond line")));

    FindAttempt attempt = tracer.drain().get(0);

    assertThat(attempt.outcome()).isEqualTo(Outcome.ERROR);
    assertThat(attempt.detail()).contains("boom").doesNotContain("second line");
  }

  @Test
  public void ignoresErrorsOfOtherMethods() throws Exception {
    tracer.beforeFindElement(driver, by);
    tracer.onError(driver, WebDriver.class.getMethod("getTitle"), new Object[0],
        new InvocationTargetException(new IllegalStateException("x")));

    assertThat(tracer.drain()).isEmpty();
  }

  @Test
  public void emptyFindElementsIsNotFound() {
    tracer.beforeFindElements(driver, by);
    tracer.afterFindElements(driver, by, List.of());

    assertThat(tracer.drain()).extracting(FindAttempt::outcome).containsExactly(Outcome.NOT_FOUND);
  }

  @Test
  public void nonEmptyFindElementsIsFound() {
    tracer.beforeFindElements(driver, by);
    tracer.afterFindElements(driver, by, List.of(element));

    assertThat(tracer.drain()).extracting(FindAttempt::outcome).containsExactly(Outcome.FOUND);
  }

  @Test
  public void recordsWebElementTargets() {
    WebElement parent = mock(WebElement.class);
    tracer.beforeFindElement(parent, by);
    tracer.afterFindElement(parent, by, element);
    tracer.beforeFindElements(parent, by);
    tracer.afterFindElements(parent, by, List.of());

    assertThat(tracer.drain()).extracting(FindAttempt::outcome).containsExactly(Outcome.FOUND, Outcome.NOT_FOUND);
  }

  @Test
  public void totalSurvivesDrainAndResets() {
    tracer.beforeFindElement(driver, by);
    tracer.afterFindElement(driver, by, element);
    tracer.drain();
    tracer.beforeFindElement(driver, by);
    tracer.afterFindElement(driver, by, element);

    assertThat(tracer.totalAttempts()).isEqualTo(2);
    tracer.resetTotal();
    assertThat(tracer.totalAttempts()).isZero();
  }

  @Test
  public void attemptsAreThreadLocal() throws Exception {
    tracer.beforeFindElement(driver, by);
    tracer.afterFindElement(driver, by, element);

    List<FindAttempt> other = CompletableFuture.supplyAsync(tracer::drain).get();

    assertThat(other).isEmpty();
    assertThat(tracer.drain()).hasSize(1);
  }

  @Test
  public void clearDiscardsFinishedAndStalePendingButKeepsTotal() {
    tracer.beforeFindElement(driver, by);
    tracer.afterFindElement(driver, by, element);
    tracer.beforeFindElement(driver, by);

    tracer.clear();
    tracer.afterFindElement(driver, by, element);

    assertThat(tracer.drain()).isEmpty();
    assertThat(tracer.totalAttempts()).isEqualTo(1);
  }

  @Test
  public void recordsOnlyWhileGateIsOpen() {
    boolean[] open = {false};
    FindTracer gated = new FindTracer(clock, () -> open[0]);
    gated.beforeFindElement(driver, by);
    gated.afterFindElement(driver, by, element);
    open[0] = true;
    gated.beforeFindElement(driver, by);
    gated.afterFindElement(driver, by, element);

    assertThat(gated.drain()).hasSize(1);
    assertThat(gated.totalAttempts()).isEqualTo(1);
  }
}
