package dev.quokkify.generator;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalUnit;

/**
 * Utility class for generating {@link LocalDateTime} instances.
 * Provides methods to generate the current date and time with customizable precision and temporal adjustments.
 * Supports generating the current {@link LocalDateTime} in a specific time zone or truncating to a specific
 * temporal unit.
 */
public class LocalDateTimeGenerator {

  public LocalDateTimeGenerator() {
  }

  public static LocalDateTime generateNowWithPrecisionSeconds() {
    return generateNowWithPrecision(ChronoUnit.SECONDS);
  }

  public static LocalDateTime generateNowWithPrecision(TemporalUnit unit) {
    return generateNow().truncatedTo(unit);
  }

  public static LocalDateTime generateNowWithAdjuster(TemporalAdjuster temporalAdjuster) {
    return generateNow().with(temporalAdjuster);
  }

  public static LocalDateTime generateNow() {
    return generateNow("Etc/UTC");
  }

  public static LocalDateTime generateNow(String zoneId) {
    return LocalDateTime.now(ZoneId.of(zoneId));
  }
}
