package dev.quokkify.elements.table.model;

import java.util.List;
import java.util.Objects;

import com.codeborne.selenide.Driver;

/** Built-in Selenide-native table assertions. */
public final class TableAssertions {

  private TableAssertions() {
  }

  public static <C> TableAssertion<C> rowCount(int expected) {
    return assertion("row count " + expected, (driver, table) -> table.rowCount() == expected);
  }

  public static <C> TableAssertion<C> headers(String... expected) {
    List<String> required = List.of(expected);
    return assertion("ordered headers " + required,
        (driver, table) -> Objects.equals(table.headers(), required));
  }

  public static <C> TableAssertion<C> matchingRow(RowAssertion<C> rowAssertion) {
    RowAssertion<C> required = Objects.requireNonNull(rowAssertion, "rowAssertion");
    return assertion("matching row: " + required.description(),
        (driver, table) -> table.rows().stream().anyMatch(row -> required.test(driver, row)));
  }

  public static <C> TableAssertion<C> columnExists(C column) {
    return assertion("column exists: key=" + column,
        (driver, table) -> table.columnIndex(column) >= 0);
  }

  private static <C> TableAssertion<C> assertion(String description,
                                                  AssertionPredicate<C> predicate) {
    return new TableAssertion<>() {
      @Override
      public String description() {
        return description;
      }

      @Override
      public boolean test(Driver driver, TableAssertionContext<C> table) {
        return predicate.test(driver, table);
      }
    };
  }

  @FunctionalInterface
  private interface AssertionPredicate<C> {
    boolean test(Driver driver, TableAssertionContext<C> table);
  }
}
