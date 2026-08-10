package dev.quokkify.util;

import java.net.ConnectException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import dev.quokkify.generator.LocalDateTimeGenerator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.awaitility.core.ThrowingRunnable;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Utilities for awaiting conditions and assertions using the Awaitility library.
 *
 * <ul>
 *   <li>Uses {@code pollInSameThread()} — do not combine with conditions that may wait indefinitely.</li>
 *   <li>Ignores {@link ConnectException} during polling.</li>
 *   <li>Prefers {@link Duration}-based overloads; integer overloads kept for convenience.</li>
 * </ul>
 */
public final class Waiter {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(5);
  private static final Duration QUICK_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration QUICK_POLLING_INTERVAL = Duration.ofMillis(500);
  private static final Duration ACTION_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration SECOND_TICK_POLLING_INTERVAL = Duration.ofMillis(100);
  private static final Duration ONE_SECOND_POLLING_INTERVAL = Duration.ofSeconds(1);

  private static final Logger LOG = LogManager.getLogger(Waiter.class);

  private Waiter() {
  }

  /**
   * Waits until the next second tick occurs and returns the start time.
   *
   * @return the time from which the wait started
   */
  public static LocalDateTime waitForNextSecond() {
    LocalDateTime startTime = LocalDateTimeGenerator.generateNowWithPrecisionSeconds();
    awaitCondition(
        LocalDateTimeGenerator::generateNowWithPrecisionSeconds,
        Matchers.greaterThan(startTime),
        "Next second did not occur",
        QUICK_TIMEOUT,
        SECOND_TICK_POLLING_INTERVAL
    );
    return startTime;
  }

  /**
   * Runs a quick assertion with a short timeout.
   */
  public static void awaitQuickAssertion(ThrowingRunnable assertion) {
    awaitAssertion(assertion, QUICK_TIMEOUT, QUICK_POLLING_INTERVAL);
  }

  /**
   * Runs an assertion with a custom quick timeout.
   */
  public static void awaitQuickAssertion(ThrowingRunnable assertion, Duration timeout) {
    awaitAssertion(assertion, timeout, QUICK_POLLING_INTERVAL);
  }

  /**
   * Runs an assertion with a very slow default timeout.
   */
  public static void awaitAssertion(ThrowingRunnable assertion) {
    awaitAssertion(assertion, DEFAULT_TIMEOUT, DEFAULT_POLLING_INTERVAL);
  }

  /**
   * Runs an assertion with a custom timeout.
   */
  public static void awaitAssertion(ThrowingRunnable assertion, Duration timeout) {
    awaitAssertion(assertion, timeout, DEFAULT_POLLING_INTERVAL);
  }

  /**
   * Runs an assertion with a custom {@link Duration} timeout and polling interval.
   */
  public static void awaitAssertion(ThrowingRunnable assertion, Duration timeout, Duration pollingInterval) {
    Awaitility.await()
        .atMost(timeout)
        .pollInterval(pollingInterval)
        .pollDelay(Duration.ZERO)
        .pollInSameThread()
        .untilAsserted(assertion);
  }

  /**
   * Waits quickly for a boolean condition.
   */
  public static void awaitQuickCondition(Callable<Boolean> condition, String errorMessage) {
    awaitCondition(condition, errorMessage, QUICK_TIMEOUT, ONE_SECOND_POLLING_INTERVAL);
  }

  /**
   * Core boolean condition await with integer time units (kept for compatibility).
   */
  public static void awaitCondition(Callable<Boolean> condition, String errorMessage,
                                    int timeoutSeconds, int pollingIntervalMillis) {
    awaitCondition(condition, errorMessage,
        Duration.ofSeconds(timeoutSeconds), Duration.ofMillis(pollingIntervalMillis));
  }

  /**
   * Core boolean condition await with {@link Duration}.
   */
  public static void awaitCondition(Callable<Boolean> condition, String errorMessage,
                                    Duration timeout, Duration pollingInterval) {
    try {
      Awaitility.await()
          .atMost(timeout)
          .pollInterval(pollingInterval)
          .pollDelay(Duration.ZERO)
          .pollInSameThread()
          .ignoreException(ConnectException.class)
          .until(condition);
    } catch (ConditionTimeoutException e) {
      throw buildConditionTimeoutException(errorMessage, timeout, e);
    }
  }

  /**
   * Quick await for a supplier with matcher.
   */
  public static <T> T awaitQuickCondition(Callable<T> supplier, Matcher<? super T> matcher, String errorMessage) {
    return awaitCondition(supplier, matcher, errorMessage, QUICK_TIMEOUT, QUICK_POLLING_INTERVAL);
  }

  /**
   * Very-slow await for a supplier with matcher.
   */
  public static <T> T awaitVerySlowCondition(Callable<T> supplier, Matcher<? super T> matcher, String errorMessage) {
    return awaitCondition(supplier, matcher, errorMessage, DEFAULT_TIMEOUT, DEFAULT_POLLING_INTERVAL);
  }

  /**
   * Core supplier + matcher await with integer time units (kept for compatibility).
   */
  public static <T> T awaitCondition(Callable<T> supplier, Matcher<? super T> matcher,
                                     String errorMessage, int timeoutSeconds, int pollIntervalMillis) {
    return awaitCondition(supplier, matcher, errorMessage,
        Duration.ofSeconds(timeoutSeconds), Duration.ofMillis(pollIntervalMillis));
  }

  /**
   * Core supplier + matcher await with {@link Duration}.
   */
  public static <T> T awaitCondition(Callable<T> supplier, Matcher<? super T> matcher,
                                     String errorMessage, Duration timeout, Duration pollingInterval) {
    try {
      return Awaitility.await()
          .atMost(timeout)
          .pollInterval(pollingInterval)
          .pollDelay(Duration.ZERO)
          .pollInSameThread()
          .ignoreException(ConnectException.class)
          .until(supplier, matcher);
    } catch (ConditionTimeoutException e) {
      throw buildConditionTimeoutException(errorMessage, timeout, e);
    }
  }

  /**
   * Waits for a condition while running an action at each poll.
   */
  public static void awaitConditionWithAction(Callable<Boolean> condition, Runnable action, String errorMessage) {
    awaitConditionWithAction(condition, action, errorMessage, ACTION_TIMEOUT, ONE_SECOND_POLLING_INTERVAL);
  }

  /**
   * Waits for a condition while running an action at each poll with custom timing.
   */
  public static void awaitConditionWithAction(Callable<Boolean> condition, Runnable action, String errorMessage,
                                              Duration timeout, Duration pollingInterval) {
    Callable<Boolean> conditionWithAction = () -> {
      action.run();
      return condition.call();
    };
    awaitCondition(conditionWithAction, errorMessage, timeout, pollingInterval);
  }

  /**
   * Asserts that the condition never becomes {@code true} during the entire timeout window.
   * Polls at the given interval; fails immediately if the condition flips to {@code true}.
   *
   * <pre>
   * Timeline (timeout = 5s, poll = 1s):
   *
   *  t=0s  t=1s  t=2s  t=3s  t=4s  t=5s
   *   |-----|-----|-----|-----|-----|
   *   F     F     F     F     F     F   -> PASS (condition stayed false the whole time)
   *   F     F     T                     -> FAIL at t=2s (condition became true)
   * </pre>
   *
   * @param condition       checked at every polling interval
   * @param timeout         how long to observe
   * @param pollingInterval how often to check
   * @param failMessage     message for the {@link AssertionError} if condition becomes {@code true}
   */
  public static void assertNeverTrue(BooleanSupplier condition,
                                     Duration timeout,
                                     Duration pollingInterval,
                                     String failMessage) {
    try {
      Awaitility.await()
          .atMost(timeout)
          .pollInterval(pollingInterval)
          .pollDelay(Duration.ZERO)
          .pollInSameThread()
          .until(condition::getAsBoolean);
      throw new AssertionError(failMessage);
    } catch (ConditionTimeoutException ignored) {
    }
  }

  /**
   * Asserts that the condition never becomes {@code true} during the entire timeout window.
   * Uses default timeout (60s) and polling interval (1000ms).
   *
   * @param condition   checked at every polling interval
   * @param failMessage message for the {@link AssertionError} if condition becomes {@code true}
   */
  public static void assertNeverTrue(BooleanSupplier condition, String failMessage) {
    assertNeverTrue(condition, DEFAULT_TIMEOUT, ONE_SECOND_POLLING_INTERVAL, failMessage);
  }

  /**
   * Asserts that the condition stays {@code true} during the entire timeout window.
   * Polls at the given interval; fails immediately if the condition drops to {@code false}.
   *
   * <pre>
   * Timeline (timeout = 5s, poll = 1s):
   *
   *  t=0s  t=1s  t=2s  t=3s  t=4s  t=5s
   *   |-----|-----|-----|-----|-----|
   *   T     T     T     T     T     T   -> PASS (condition stayed true the whole time)
   *   T     T     F                     -> FAIL at t=2s (condition dropped to false)
   * </pre>
   *
   * @param condition       checked at every polling interval
   * @param timeout         how long to observe
   * @param pollingInterval how often to check
   * @param failMessage     message for the {@link AssertionError} if condition drops to {@code false}
   */
  public static void assertAlwaysTrue(BooleanSupplier condition,
                                      Duration timeout,
                                      Duration pollingInterval,
                                      String failMessage) {
    assertNeverTrue(() -> !condition.getAsBoolean(), timeout, pollingInterval, failMessage);
  }

  /**
   * Asserts that the condition stays {@code true} during the entire timeout window.
   * Uses default timeout (60s) and polling interval (1000ms).
   *
   * @param condition   checked at every polling interval
   * @param failMessage message for the {@link AssertionError} if condition drops to {@code false}
   */
  public static void assertAlwaysTrue(BooleanSupplier condition, String failMessage) {
    assertAlwaysTrue(condition, DEFAULT_TIMEOUT, ONE_SECOND_POLLING_INTERVAL, failMessage);
  }

  /**
   * Sleeps current thread for the given milliseconds, restoring interrupt flag if interrupted.
   */
  public static void threadSleep(long millis) {
    LOG.debug("Wait for '{}' seconds", TimeUnit.MILLISECONDS.toSeconds(millis));
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt(); // restore interrupt flag
      LOG.warn("Thread was interrupted during sleep ({} ms)", millis);
    }
  }

  private static ConditionTimeoutException buildConditionTimeoutException(
      String message, Duration timeout, ConditionTimeoutException cause) {
    String msg = "%s, within '%d' seconds".formatted(message, timeout.toSeconds());
    return new ConditionTimeoutException(msg, cause);
  }
}
