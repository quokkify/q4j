package dev.quokkify.elements.table.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/** Built-in typed row conditions. Numeric comparisons use strict {@link BigDecimal} syntax. */
public final class RowConditions {

  private RowConditions() {
  }

  public static <C> RowCondition<C> exact(C column, String expected) {
    Objects.requireNonNull(expected, "expected");
    return row -> row.cell(column).map(cell -> cell.text().equals(expected)).orElse(false);
  }

  public static <C> RowCondition<C> contains(C column, String fragment) {
    Objects.requireNonNull(fragment, "fragment");
    return row -> row.cell(column).map(cell -> cell.text().contains(fragment)).orElse(false);
  }

  public static <C> RowCondition<C> regex(C column, Pattern pattern) {
    Objects.requireNonNull(pattern, "pattern");
    return row -> row.cell(column).map(cell -> pattern.matcher(cell.text()).matches()).orElse(false);
  }

  public static <C> RowCondition<C> regex(C column, String pattern) {
    return regex(column, Pattern.compile(Objects.requireNonNull(pattern, "pattern")));
  }

  public static <C> RowCondition<C> greaterThan(C column, Number expected) {
    // Preserve predicate compatibility: invalid cell text is a non-match, not an exception.
    BigDecimal threshold = new BigDecimal(Objects.requireNonNull(expected, "expected").toString());
    return row -> row.cell(column).map(TableCell::text).map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(value -> isGreaterThan(value, threshold))
        .orElse(false);
  }

  @SafeVarargs
  public static <C> RowCondition<C> all(RowCondition<C>... conditions) {
    Objects.requireNonNull(conditions, "conditions");
    RowCondition<C>[] copy = Arrays.copyOf(conditions, conditions.length);
    Arrays.stream(copy).forEach(condition -> Objects.requireNonNull(condition, "condition"));
    return row -> Arrays.stream(copy).allMatch(condition -> condition.test(row));
  }

  private static boolean isGreaterThan(String actual, BigDecimal expected) {
    try {
      return new BigDecimal(actual).compareTo(expected) > 0;
    } catch (NumberFormatException ignored) {
      return false;
    }
  }
}
