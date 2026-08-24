package dev.quokkify.elements.table.model;

import java.util.List;
import java.util.Objects;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.WebElementCondition;

/** Built-in row assertions evaluated within the table's single Selenide wait loop. */
public final class RowAssertions {

  private RowAssertions() {
  }

  public static <C> RowAssertion<C> values(String... expected) {
    List<String> required = List.of(expected);
    return assertion("ordered row values " + required,
        (driver, row) -> Objects.equals(row.values(), required));
  }

  public static <C> RowAssertion<C> cell(int columnIndex, WebElementCondition condition) {
    if (columnIndex < 0) {
      throw new IndexOutOfBoundsException(columnIndex);
    }
    WebElementCondition required = Objects.requireNonNull(condition, "condition");
    return assertion("cell index " + columnIndex + " " + required,
        (driver, row) -> columnIndex < row.values().size()
            && accepted(required.check(driver, row.cell(columnIndex))));
  }

  public static <C> RowAssertion<C> cell(C column, WebElementCondition condition) {
    WebElementCondition required = Objects.requireNonNull(condition, "condition");
    return assertion("cell key " + column + " " + required, (driver, row) -> {
      int columnIndex = row.columnIndex(column);
      return columnIndex >= 0 && columnIndex < row.values().size()
          && accepted(required.check(driver, row.cell(columnIndex)));
    });
  }

  private static boolean accepted(CheckResult result) {
    return result.verdict() == CheckResult.Verdict.ACCEPT;
  }

  private static <C> RowAssertion<C> assertion(String description,
                                                AssertionPredicate<C> predicate) {
    return new RowAssertion<>() {
      @Override
      public String description() {
        return description;
      }

      @Override
      public boolean test(Driver driver, RowAssertionContext<C> row) {
        return predicate.test(driver, row);
      }
    };
  }

  @FunctionalInterface
  private interface AssertionPredicate<C> {
    boolean test(Driver driver, RowAssertionContext<C> row);
  }
}
