package dev.quokkify.elements.table.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable object representing an expected value in a table row.
 *
 * @param <T> enum with column enumerations
 */
public record ExpectedValue<T extends Enum<T>>(
    CheckType checkType,
    T columnHeader,
    String cellValue,
    LocalDate cellLocalDateValue,
    LocalDateTime cellLocalDateTimeValue,
    DateTimeFormatter dateTimeFormatter
) {

  public ExpectedValue {
    if (checkType == null) {
      throw new IllegalArgumentException("checkType cannot be null");
    }
    if (columnHeader == null) {
      throw new IllegalArgumentException("columnHeader cannot be null");
    }
  }

  public static <T extends Enum<T>> ExpectedValue<T> ofString(CheckType type, T column, String value) {
    return new ExpectedValue<>(type, column, value, null, null, null);
  }

  public static <T extends Enum<T>> ExpectedValue<T> ofDate(CheckType type,
                                                            T column,
                                                            LocalDate date,
                                                            DateTimeFormatter formatter) {
    return new ExpectedValue<>(type, column, null, date, null, formatter);
  }

  public static <T extends Enum<T>> ExpectedValue<T> ofDateTime(CheckType type,
                                                                T column,
                                                                LocalDateTime dateTime,
                                                                DateTimeFormatter formatter) {
    return new ExpectedValue<>(type, column, null, null, dateTime, formatter);
  }

  public static <T extends Enum<T>> ExpectedValue<T> ofEmpty(CheckType type, T column) {
    return new ExpectedValue<>(type, column, null, null, null, null);
  }

  public static <T extends Enum<T>> ExpectedValue<T> ofInteger(CheckType type, T column, Integer value) {
    return new ExpectedValue<>(type, column, value != null ? String.valueOf(value) : null, null, null, null);
  }

  public static <T extends Enum<T>> ExpectedValue<T> ofLong(CheckType type, T column, Long value) {
    return new ExpectedValue<>(type, column, value != null ? String.valueOf(value) : null, null, null, null);
  }

  public static <T extends Enum<T>> ExpectedValue<T> ofDouble(CheckType type, T column, Double value) {
    return new ExpectedValue<>(type, column, value != null ? String.valueOf(value) : null, null, null, null);
  }

}
