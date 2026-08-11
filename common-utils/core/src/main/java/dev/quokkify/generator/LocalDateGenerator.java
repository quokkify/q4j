package dev.quokkify.generator;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjuster;

/**
 * Utility class for generating {@link LocalDate} instances.
 * This class provides methods to generate the current date based on a default or custom time zone,
 * as well as applying temporal adjusters to manipulate the generated date.
 */
public class LocalDateGenerator {

  private LocalDateGenerator() {
  }

  /**
   * Generates the current {@link LocalDate} and applies a specified {@link TemporalAdjuster}.
   * This allows for further modification of the generated date, such as adjusting to the specific day of the week
   * or the month.
   *
   * @param temporalAdjuster the {@link TemporalAdjuster} to apply (e.g., {@link java.time.temporal.TemporalAdjusters})
   * @return the adjusted {@link LocalDate}
   */
  public static LocalDate generateNowWithAdjuster(TemporalAdjuster temporalAdjuster) {
    return generateNow().with(temporalAdjuster);
  }

  /**
   * Generates the current {@link LocalDate} based on the default time zone.
   *
   * @return the current date based on the default time zone
   */
  public static LocalDate generateNow() {
    return generateNow("Etc/UTC");
  }

  /**
   * Generates the current {@link LocalDate} for a specified time zone.
   *
   * @param zoneId the time zone ID as a string
   * @return the current date in the specified time zone
   */
  public static LocalDate generateNow(String zoneId) {
    return LocalDate.now(ZoneId.of(zoneId));
  }
}
