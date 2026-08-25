package dev.quokkify.elements.table.model;

import com.codeborne.selenide.Driver;

/** One assertion evaluated against a current row from the same table snapshot. */
public interface RowAssertion<C> {

  String description();

  boolean test(Driver driver, RowAssertionContext<C> row);

  /** Combines row assertions within the same authoritative table poll. */
  default RowAssertion<C> and(RowAssertion<C> other) {
    RowAssertion<C> required = java.util.Objects.requireNonNull(other, "other");
    RowAssertion<C> current = this;
    return new RowAssertion<>() {
      @Override
      public String description() {
        return current.description() + " and " + required.description();
      }

      @Override
      public boolean test(Driver driver, RowAssertionContext<C> row) {
        return current.test(driver, row) && required.test(driver, row);
      }
    };
  }
}
