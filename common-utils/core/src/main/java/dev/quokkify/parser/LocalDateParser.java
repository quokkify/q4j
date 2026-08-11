package dev.quokkify.parser;

import java.time.LocalDate;

/**
 * Utility class for parsing date strings into {@link LocalDate} instances.
 *
 * <p>Example usage:</p>
 * <pre>
 *     LocalDate date = LocalDateParser.toLocalDate("2024-10-10");
 * </pre>
 */
public class LocalDateParser {

  private LocalDateParser() {
  }

  public static LocalDate toLocalDate(String dateString) {
    return LocalDate.parse(dateString);
  }
}
