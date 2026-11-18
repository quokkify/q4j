package dev.quokkify.elements.table;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.WebElementCondition;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

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
   * The texts of {@code headers} in every row, in row order, are exactly {@code expected}.
   */
  public static TableCondition exactRows(List<String> headers, List<List<String>> expected) {
    List<String> columns = List.copyOf(headers);
    List<List<String>> rows = expected.stream().map(List::copyOf).toList();
    return new TableCondition("rows " + columns + " (" + rows.size() + " rows)", null, snap -> snap.texts(columns),
        rows);
  }

  /**
   * The rows keyed by the text of {@code keyHeader} have exactly the {@code items} found in their {@code valuesHeader}
   * cell: same keys in any row order, same items in displayed order. Keys must be unique.
   */
  public static TableCondition exactValuesBy(String keyHeader, String valuesHeader, By items,
      Map<String, List<String>> expected) {
    Map<String, List<String>> values = Map.copyOf(expected.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue()))));
    return new TableCondition("\"" + valuesHeader + "\" items by \"" + keyHeader + "\" (" + values.size() + " rows)",
        SnapshotLocator.of(Objects.requireNonNull(items, "items")), snap -> snap.valuesBy(keyHeader, valuesHeader),
        values);
  }

  /**
   * A custom expectation: {@code actual} applied to the snapshot equals {@code expected}.
   */
  public static <T> TableCondition of(String description, Function<TableSnapshot, T> actual, T expected) {
    return new TableCondition(description, null, actual, expected);
  }

  /**
   * Like {@link #of(String, Function, Object)}, reading the texts of {@code items} inside every cell, as
   * {@link Table#snapshot(By)} does.
   */
  public static <T> TableCondition of(String description, By items, Function<TableSnapshot, T> actual, T expected) {
    return new TableCondition(description, SnapshotLocator.of(Objects.requireNonNull(items, "items")), actual,
        expected);
  }

  /**
   * Compares the snapshot with the expectation. A header that is missing or ambiguous, or a duplicate key, rejects the
   * check instead of throwing, so a table that is still rendering is retried.
   */
  CheckResult evaluate(TableSnapshot snapshot) {
    Object value;
    try {
      value = actual.apply(snapshot);
    } catch (TableColumnException | IllegalStateException e) {
      return CheckResult.rejected(e.getMessage(), e.getMessage());
    }
    if (expected.equals(value)) {
      return CheckResult.accepted(value);
    }
    String difference = TableDiff.describe(expected, value);
    return CheckResult.rejected(difference, difference);
  }

  WebElementCondition bind(Table table) {
    List<List<String>> locators = table.scriptLocators(items);
    return new WebElementCondition(description) {
      @Override
      public CheckResult check(Driver driver, WebElement root) {
        Object[] args = Table.scriptArgs(root, locators);
        return evaluate(TableSnapshot.fromScript(driver.executeJavaScript(SnapshotScript.SOURCE, args),
            table.root().toString()));
      }
    };
  }

  @Override
  public String toString() {
    return description;
  }
}
