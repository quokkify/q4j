package dev.quokkify.elements.table.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import dev.quokkify.formatter.NumberFormatter;

import org.apache.commons.lang3.StringUtils;

/**
 * Object to form the expected values in the table row.
 *
 * @param <T> enum with columns enumerations
 */
public class RowData<T extends Enum<T>> {

  private List<ExpectedValue<T>> expectedValues;

  private RowData(List<ExpectedValue<T>> expectedValues) {
    this.expectedValues = List.copyOf(expectedValues);
  }

  public List<ExpectedValue<T>> getExpectedValues() {
    return expectedValues;
  }

  public static <T extends Enum<T>> Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Builder to form the expected values in the table row.
   *
   * @param <T> enum with columns enumerations
   */
  public static class Builder<T extends Enum<T>> {

    private final List<ExpectedValue<T>> expectedValues = new ArrayList<>();

    public Builder<T> addOrEmptyIfNull(T columnName, LocalDateTime cellValue, DateTimeFormatter formatter) {
      return addOrDefaultIfNull(columnName, cellValue, formatter, StringUtils.EMPTY);
    }

    public Builder<T> addOrEmptyIfNull(T columnName, LocalDate cellValue, DateTimeFormatter formatter) {
      return addOrDefaultIfNull(columnName, cellValue, formatter, StringUtils.EMPTY);
    }

    public Builder<T> addOrEmptyIfNull(T columnName, Integer cellValue) {
      return addOrDefaultIfNull(columnName, cellValue, StringUtils.EMPTY);
    }

    public Builder<T> addOrEmptyIfNull(T columnName, Double cellValue) {
      return addOrDefaultIfNull(columnName, cellValue, StringUtils.EMPTY);
    }

    public Builder<T> addOrEmptyIfNull(T columnName, Long cellValue) {
      return addOrDefaultIfNull(columnName, cellValue, StringUtils.EMPTY);
    }

    public Builder<T> addOrEmptyIfNull(T columnName, String cellValue) {
      return addOrDefaultIfNull(columnName, cellValue, StringUtils.EMPTY);
    }

    public Builder<T> addOrEmptyIfNull(T columnName, Object conditionValue, String cellValue) {
      return addOrDefaultIfNull(columnName, conditionValue, cellValue, StringUtils.EMPTY);
    }

    public Builder<T> addOrEmptyIfNull(T columnName, Object conditionValue, Supplier<String> cellValueSupplier) {
      return addOrDefaultIfNull(columnName, conditionValue, cellValueSupplier, StringUtils.EMPTY);
    }

    public Builder<T> addOrDefaultIfNull(T columnName, LocalDateTime cellValue, DateTimeFormatter formatter,
                                         String cellDefaultValue) {
      return Objects.nonNull(cellValue) ? add(columnName, cellValue, formatter) : add(columnName, cellDefaultValue);

    }

    public Builder<T> addOrDefaultIfNull(T columnName, LocalDate cellValue, DateTimeFormatter formatter,
                                         String cellDefaultValue) {
      return Objects.nonNull(cellValue) ? add(columnName, cellValue, formatter) : add(columnName, cellDefaultValue);
    }

    public Builder<T> addOrDefaultIfNull(T columnName, Integer cellValue, String cellDefaultValue) {
      return addOrDefaultIfNull(columnName, NumberFormatter.formatNumberIfNull(cellValue), cellDefaultValue);
    }

    public Builder<T> addOrDefaultIfNull(T columnName, Double cellValue, String cellDefaultValue) {
      return addOrDefaultIfNull(columnName, NumberFormatter.formatNumberIfNull(cellValue), cellDefaultValue);
    }

    public Builder<T> addOrDefaultIfNull(T columnName, Long cellValue, String cellDefaultValue) {
      return addOrDefaultIfNull(columnName, NumberFormatter.formatNumberIfNull(cellValue), cellDefaultValue);
    }

    public Builder<T> addOrDefaultIfNull(T columnName, String cellValue, String cellDefaultValue) {
      return add(columnName, Objects.nonNull(cellValue) ? cellValue : cellDefaultValue);
    }

    public Builder<T> addOrDefaultIfNull(T columnName, Object conditionValue, String cellValueIfNonNull,
                                         String cellValueIfNull) {
      return add(columnName, Objects.nonNull(conditionValue) ? cellValueIfNonNull : cellValueIfNull);
    }

    public Builder<T> addOrDefaultIfNull(T columnName, Object conditionValue, Supplier<String> cellValueIfNonNull,
                                         String cellValueIfNull) {
      return add(columnName, Objects.nonNull(conditionValue) ? cellValueIfNonNull.get() : cellValueIfNull);
    }

    public Builder<T> addEmpty(T columnName) {
      return add(CheckType.EMPTY, columnName, StringUtils.EMPTY);
    }

    public Builder<T> addLink(T columnName, String cellLinkValue) {
      expectedValues.add(ExpectedValue.ofString(CheckType.LINK, columnName, cellLinkValue));
      return this;
    }

    public Builder<T> add(T columnName, LocalDateTime cellValue, DateTimeFormatter formatter) {
      expectedValues.add(ExpectedValue.ofDateTime(CheckType.DATE_TIME, columnName, cellValue, formatter));
      return this;
    }

    public Builder<T> add(T columnName, LocalDate cellValue, DateTimeFormatter formatter) {
      expectedValues.add(ExpectedValue.ofDate(CheckType.DATE, columnName, cellValue, formatter));
      return this;
    }

    public Builder<T> add(CheckType checkType, T columnName, String cellValue) {
      expectedValues.add(ExpectedValue.ofString(checkType, columnName, cellValue));
      return this;
    }

    public Builder<T> add(T columnName, Integer cellValue) {
      expectedValues.add(ExpectedValue.ofInteger(CheckType.EQUALS, columnName, cellValue));
      return this;
    }

    public Builder<T> add(T columnName, Double cellValue) {
      expectedValues.add(ExpectedValue.ofDouble(CheckType.EQUALS, columnName, cellValue));
      return this;
    }

    public Builder<T> add(T columnName, Long cellValue) {
      expectedValues.add(ExpectedValue.ofLong(CheckType.EQUALS, columnName, cellValue));
      return this;
    }

    public Builder<T> add(T columnName, String cellValue) {
      return add(CheckType.EQUALS, columnName, cellValue);
    }

    public Builder<T> addContains(T columnName, String cellContainsValue) {
      return add(CheckType.CONTAINS, columnName, cellContainsValue);
    }

    public Builder<T> addStartsWith(T columnName, String cellStartsWithValue) {
      return add(CheckType.STARTS_WITH, columnName, cellStartsWithValue);
    }

    public Builder<T> addEndsWith(T columnName, String cellEndsWithValue) {
      return add(CheckType.ENDS_WITH, columnName, cellEndsWithValue);
    }

    public RowData<T> build() {
      return new RowData<>(expectedValues);
    }
  }
}
