package dev.quokkify.flutter.trace;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

final class MutableClock extends Clock {

  private Instant now = Instant.parse("2026-01-01T00:00:00Z");

  void advance(long millis) {
    now = now.plusMillis(millis);
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return this;
  }

  @Override
  public Instant instant() {
    return now;
  }
}
