package dev.quokkify.parser;

import java.time.LocalDateTime;

import dev.quokkify.constant.DateType;

/**
 * Utility class for parsing date-time strings into {@link LocalDateTime} instances.
 *
 * <p>Example usage:</p>
 *
 * <pre>
 *     LocalDateTime dateTime = LocalDateTimeParser.toLocalDateTime("2024-10-10T15:30", DateFormat.YYYY_MM_DD_T_HH_MM);
 * </pre>
 */
public class LocalDateTimeParser {

  private LocalDateTimeParser() {
  }

  public static LocalDateTime toLocalDateTime(String dateString, DateType dateType) {
    return LocalDateTime.from(dateType.getFormatter().parse(dateString));
  }
}
