package dev.quokkify.elements.table.model;

import java.util.Objects;

/** Composable condition evaluated against a lazily resolved query row. */
@FunctionalInterface
public interface RowCondition<C> {

  boolean test(TableQueryRow<C> row);

  /** Combines conditions with logical AND and short-circuit evaluation. */
  default RowCondition<C> and(RowCondition<C> other) {
    RowCondition<C> required = Objects.requireNonNull(other, "other");
    return row -> test(row) && required.test(row);
  }
}
