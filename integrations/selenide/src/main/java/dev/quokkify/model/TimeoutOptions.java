package dev.quokkify.model;

import java.time.Duration;
import java.util.Objects;

/** Immutable timeout and polling settings for a verification chain. */
public final class TimeoutOptions {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration DEFAULT_POLLING = Duration.ofMillis(500);

  private final Duration timeout;
  private final Duration pollingInterval;

  /** Creates a complete set of timeout options. */
  public TimeoutOptions(Duration timeout, Duration pollingInterval) {
    this.timeout = positive(timeout, "timeout");
    this.pollingInterval = positive(pollingInterval, "pollingInterval");
  }

  private TimeoutOptions(Duration timeout, Duration pollingInterval, boolean partial) {
    this.timeout = timeout == null ? null : positive(timeout, "timeout");
    this.pollingInterval = pollingInterval == null ? null : positive(pollingInterval, "pollingInterval");
  }

  /** Returns the default ten-second timeout and 500 ms polling interval. */
  public static TimeoutOptions defaults() {
    return new TimeoutOptions(DEFAULT_TIMEOUT, DEFAULT_POLLING);
  }

  /** Returns options that override only the timeout. */
  public static TimeoutOptions timeout(Duration timeout) {
    return new TimeoutOptions(positive(timeout, "timeout"), null, true);
  }

  /** Returns options that override only the polling interval. */
  public static TimeoutOptions polling(Duration pollingInterval) {
    return new TimeoutOptions(null, positive(pollingInterval, "pollingInterval"), true);
  }

  public Duration getTimeout() {
    return timeout;
  }

  public Duration getPollingInterval() {
    return pollingInterval;
  }

  private static Duration positive(Duration value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return value;
  }
}
