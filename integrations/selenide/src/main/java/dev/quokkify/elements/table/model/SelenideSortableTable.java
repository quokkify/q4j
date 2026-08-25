package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class SelenideSortableTable<C> implements SortableTable<C> {

  private final SelenideTableQuery<C> query;
  private final TableSorting<C> sorting;

  public SelenideSortableTable(SelenideTableQuery<C> query, TableSortingSpec<C> spec) {
    this.query = Objects.requireNonNull(query, "query");
    Objects.requireNonNull(spec, "spec");
    this.sorting = new Sorting(spec);
  }

  @Override
  public SelenideTableQuery<C> query() {
    return query;
  }

  @Override
  public TableSorting<C> sorting() {
    return sorting;
  }

  private final class Sorting implements TableSorting<C> {
    private final TableSortingSpec<C> spec;

    private Sorting(TableSortingSpec<C> spec) {
      this.spec = spec;
    }

    @Override
    public SortableTable<C> sortBy(C column, TableSortDirection direction) {
      return sortBy(column, direction, TableCapabilityStateWaiter.defaultTimeout());
    }

    @Override
    public SortableTable<C> sortBy(C column, TableSortDirection direction, Duration timeout) {
      C requiredColumn = Objects.requireNonNull(column, "column");
      Objects.requireNonNull(direction, "direction");
      Optional<TableSortDirection> current = currentDirection(requiredColumn);
      if (current.isPresent() && Objects.equals(current.get(), direction)) {
        return SelenideSortableTable.this;
      }
      TableCapabilityStateWaiter.perform(
          "sorting by " + requiredColumn + " " + direction,
          spec.stateToken(),
          () -> spec.control().apply(requiredColumn).click(),
          timeout);
      current = currentDirection(requiredColumn);
      if (current.isPresent() && !Objects.equals(current.get(), direction)) {
        TableCapabilityStateWaiter.perform(
            "sorting by " + requiredColumn + " " + direction,
            spec.stateToken(),
            () -> spec.control().apply(requiredColumn).click(),
            timeout);
      }
      return SelenideSortableTable.this;
    }

    @Override
    public Optional<TableSortDirection> currentDirection(C column) {
      return spec.currentDirection().apply(Objects.requireNonNull(column, "column"));
    }
  }
}
