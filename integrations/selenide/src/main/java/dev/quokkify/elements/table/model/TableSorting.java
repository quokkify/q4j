package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.Optional;

/** Sorting actions and observable state for an opt-in table capability. */
public interface TableSorting<C> {

  SortableTable<C> sortBy(C column, TableSortDirection direction);

  SortableTable<C> sortBy(C column, TableSortDirection direction, Duration timeout);

  Optional<TableSortDirection> currentDirection(C column);
}
