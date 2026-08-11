package dev.quokkify.formatter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import dev.quokkify.constant.DateType;

/**
 * Utility class for formatting {@link LocalDate} objects into strings based on specified patterns and locales.
 */
public class LocalDateFormatter {

  private LocalDateFormatter() {
  }

  /**
   * Formats a {@link LocalDate} using the pattern and locale provided by a {@link DateType} enumeration.
   *
   * @param localDate  The {@link LocalDate} object to be formatted.
   * @param dateFormat The {@link DateType} enum, which contains the pattern and default locale for formatting.
   * @return A formatted string representation of the {@link LocalDate}.
   */
  public static String format(LocalDate localDate, DateType dateFormat) {
    return format(localDate, dateFormat.getPattern(), dateFormat.getDefaultLocale());
  }

  /**
   * Formats a {@link LocalDate} using the specified pattern and locale.
   *
   * @param localDate The {@link LocalDate} object to be formatted.
   * @param pattern   The pattern describing the date format, e.g., "yyyy-MM-dd".
   * @param locale    The {@link Locale} to use for formatting.
   * @return A formatted string representation of the {@link LocalDate}.
   */
  public static String format(LocalDate localDate, String pattern, Locale locale) {
    return localDate.format(DateTimeFormatter.ofPattern(pattern, locale));
  }
}
