package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.List;

/** Lazy reference to a zero-based logical table column. */
public interface TableColumnRef<C> {

  int index();

  List<? extends TableCellRef<C>> cells();

  /** Waits for a condition against the lazily re-resolved column. */
  default TableColumnRef<C> shouldHave(ColumnAssertion condition) {
    throw new UnsupportedOperationException(
        "shouldHave requires a Selenide element-backed table column reference");
  }

  /** Waits for a condition against the lazily re-resolved column. */
  default TableColumnRef<C> shouldHave(ColumnAssertion condition, Duration timeout) {
    throw new UnsupportedOperationException(
        "shouldHave requires a Selenide element-backed table column reference");
  }
}
