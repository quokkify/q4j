package dev.quokkify.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.awaitility.core.ConditionTimeoutException;
import org.testng.annotations.Test;

public class WaiterAwaitAssertionTest {

  @Test
  public void awaitAssertionStopsPollingAsSoonAsAssertionPasses() {
    AtomicInteger attempts = new AtomicInteger();

    Waiter.awaitAssertion(
        () -> Assertions.assertThat(attempts.incrementAndGet()).isGreaterThanOrEqualTo(3),
        Duration.ofSeconds(5),
        Duration.ofMillis(50));

    Assertions.assertThat(attempts.get()).isEqualTo(3);
  }

  @Test
  public void awaitAssertionHonoursCustomTimeoutInsteadOfDefaultSixtySeconds() {
    Duration customTimeout = Duration.ofMillis(500);
    Instant start = Instant.now();

    Assertions.assertThatThrownBy(() -> Waiter.awaitAssertion(
            () -> Assertions.assertThat(true).isFalse(),
            customTimeout,
            Duration.ofMillis(100)))
        .isInstanceOf(ConditionTimeoutException.class);

    Duration elapsed = Duration.between(start, Instant.now());
    Assertions.assertThat(elapsed).isBetween(customTimeout, Duration.ofSeconds(3));
  }

  @Test
  public void awaitAssertionHonoursCustomPollingIntervalRatherThanDefaultFiveSeconds() {
    AtomicInteger pollCount = new AtomicInteger();

    Assertions.assertThatThrownBy(() -> Waiter.awaitAssertion(
            () -> {
              pollCount.incrementAndGet();
              Assertions.assertThat(true).isFalse();
            },
            Duration.ofSeconds(2),
            Duration.ofMillis(500)))
        .isInstanceOf(ConditionTimeoutException.class);

    Assertions.assertThat(pollCount.get()).isBetween(3, 6);
  }
}
