package dev.quokkify.elements.table.model;

import java.util.List;
import java.util.Objects;

/** Built-in lazy column assertions. */
public final class ColumnAssertions {

  private ColumnAssertions() {
  }

  /** Requires values in exact DOM order, retaining duplicates. */
  public static ColumnAssertion values(String... expected) {
    List<String> required = List.of(expected);
    return new ColumnAssertion() {
      @Override
      public String description() {
        return "column values " + required;
      }

      @Override
      public boolean test(List<String> actualValues) {
        return Objects.equals(actualValues, required);
      }
    };
  }
}
