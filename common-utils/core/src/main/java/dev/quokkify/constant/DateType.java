package dev.quokkify.constant;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Interface representing a type of date format that provides locale-sensitive formatting and parsing patterns.
 *
 * <p>
 * Implementations of this interface define a specific date pattern and provide methods to retrieve a
 * {@link DateTimeFormatter} for formatting dates in the default or specified {@link Locale}.
 */
public interface DateType {

  /**
   * Returns the default {@link Locale} to be used for date formatting and parsing.
   */
  Locale getDefaultLocale();

  /**
   * Returns the date format pattern as a {@link String}, which defines how the date should be formatted or parsed.
   */
  String getPattern();

  /**
   * Returns a {@link DateTimeFormatter} using the default locale for formatting or parsing dates according
   * to the specified pattern.
   *
   * @return a {@link DateTimeFormatter} for the default locale
   */
  default DateTimeFormatter getFormatter() {
    return getFormatter(getDefaultLocale());
  }

  /**
   * Returns a {@link DateTimeFormatter} for a specified {@link Locale}, using the date pattern defined
   * in the implementation of this interface.
   *
   * @param locale the locale to use for formatting
   * @return a {@link DateTimeFormatter} for the specified locale
   */
  default DateTimeFormatter getFormatter(Locale locale) {
    return DateTimeFormatter.ofPattern(getPattern(), locale);
  }
}
