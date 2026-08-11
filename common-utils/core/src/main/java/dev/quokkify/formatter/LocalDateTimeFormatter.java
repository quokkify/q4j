package dev.quokkify.formatter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import dev.quokkify.constant.DateType;

/**
 * Utility class for formatting and truncating {@link LocalDateTime} instances.
 * This class provides methods to format {@link LocalDateTime} objects into strings based on
 * different {@link DateType} formats or custom patterns and locales. Additionally, it offers
 * various truncation methods to round down a {@link LocalDateTime} object to a specific unit of time.
 *
 * <p>This is a final class with static methods, and cannot be instantiated.</p>
 *
 * <p>Usage examples:</p>
 * <pre>
 *   LocalDateTime dateTime = LocalDateTime.now();
 *   String formatted = LocalDateTimeFormatter.format(dateTime, DateFormat.YYYY_MM_DD);
 *   LocalDateTime truncatedToSeconds = LocalDateTimeFormatter.truncatedToSeconds(dateTime);
 * </pre>
 */
public class LocalDateTimeFormatter {

  private LocalDateTimeFormatter() {
  }

  public static String format(LocalDateTime localDateTime, DateType dateFormat) {
    return format(localDateTime, dateFormat.getPattern(), dateFormat.getDefaultLocale());
  }

  public static String format(LocalDateTime localDateTime, String pattern, Locale locale) {
    return localDateTime.format(DateTimeFormatter.ofPattern(pattern, locale));
  }

  public static LocalDateTime truncatedToSeconds(LocalDateTime localDateTime) {
    return truncatedTo(localDateTime, ChronoUnit.SECONDS);
  }

  public static LocalDateTime truncatedToMinutes(LocalDateTime localDateTime) {
    return truncatedTo(localDateTime, ChronoUnit.MINUTES);
  }

  public static LocalDateTime truncatedToHours(LocalDateTime localDateTime) {
    return truncatedTo(localDateTime, ChronoUnit.HOURS);
  }

  public static LocalDateTime truncatedToDays(LocalDateTime localDateTime) {
    return truncatedTo(localDateTime, ChronoUnit.DAYS);
  }

  public static LocalDateTime truncatedToMonths(LocalDateTime localDateTime) {
    return truncatedTo(localDateTime, ChronoUnit.MONTHS);
  }

  public static LocalDateTime truncatedToYears(LocalDateTime localDateTime) {
    return truncatedTo(localDateTime, ChronoUnit.YEARS);
  }

  public static LocalDateTime truncatedTo(LocalDateTime localDateTime, ChronoUnit chronoUnit) {
    return localDateTime.truncatedTo(chronoUnit);
  }
}
