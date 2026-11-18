package dev.quokkify.elements.table;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.codeborne.selenide.CheckResult;
import org.openqa.selenium.By;

/**
 * An expectation on a whole table, checked by {@link Table#shouldHave(TableCondition)}. Every check reads the table
 * in one browser call into a {@link TableSnapshot}, extracts a value from it and compares that value with the expected
 * one by {@link Object#equals}; Selenide repeats the check until it passes or the timeout expires.
 */
public final class TableCondition {

  private final String description;
  private final SnapshotLocator items;
  private final Function<TableSnapshot, ?> actual;
  private final Object expected;

  private TableCondition(String description, SnapshotLocator items, Function<TableSnapshot, ?> actual,
      Object expected) {
    this.description = Objects.requireNonNull(description, "description");
    this.items = items;
    this.actual = Objects.requireNonNull(actual, "actual");
    this.expected = Objects.requireNonNull(expected, "expected");
  }

  /**
   * The texts of {@code headers} in every row, in row order, are exactly {@code expected}. Rows are compared by
   * position; a missing cell of a short row reads as {@code ""}, like an empty cell. Displayed texts are
   * whitespace-normalized, so write expected texts with single spaces.
   */
  public static TableCondition exactRows(List<String> headers, List<List<String>> expected) {
    List<String> columns = List.copyOf(Objects.requireNonNull(headers, "headers"));
    List<List<String>> rows = Objects.requireNonNull(expected, "expected").stream().map(List::copyOf).toList();
    return new TableCondition("rows " + columns + " (" + rows.size() + " rows)", null, snap -> snap.texts(columns),
        rows);
  }

  /**
   * The rows keyed by the text of {@code keyHeader} have exactly the {@code items} found in their {@code header}
   * cell: same keys in any row order, same items in displayed order. Keys must be unique. A failure lists missing keys
   * in the order of {@code expected} and other differences in row order.
   */
  public static TableCondition exactItemsBy(String keyHeader, String header, By items,
      Map<String, List<String>> expected) {
    Objects.requireNonNull(keyHeader, "keyHeader");
    Objects.requireNonNull(header, "header");
    Map<String, List<String>> values = new LinkedHashMap<>();
    Objects.requireNonNull(expected, "expected").forEach((key, list) -> values.put(Objects.requireNonNull(key, "key"),
        List.copyOf(list)));
    return new TableCondition("\"" + header + "\" items by \"" + keyHeader + "\" (" + values.size() + " rows)",
        SnapshotLocator.of(Objects.requireNonNull(items, "items")),
        snap -> snap.columnItemsBy(keyHeader, header),
        Collections.unmodifiableMap(values));
  }

  /**
   * Compares the snapshot with the expectation. A header that is missing or ambiguous, or a duplicate key, rejects the
   * check instead of throwing, so a table that is still rendering is retried.
   */
  CheckResult evaluate(TableSnapshot snapshot) {
    Object value;
    try {
      value = actual.apply(snapshot);
    } catch (TableColumnException | TableKeyException e) {
      return CheckResult.rejected(e.getMessage(), e.getMessage());
    }
    if (expected.equals(value)) {
      return CheckResult.accepted(value);
    }
    String difference = TableDiff.describe(expected, value);
    return CheckResult.rejected(difference, difference);
  }

  SnapshotLocator items() {
    return items;
  }

  @Override
  public String toString() {
    return description;
  }
}
