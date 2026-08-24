package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.Objects;

public final class SelenideFilterableTable<C> implements FilterableTable<C> {

  private final SelenideTableQuery<C> query;
  private final TableFiltering<C> filtering;

  public SelenideFilterableTable(SelenideTableQuery<C> query, TableFilteringSpec<C> spec) {
    this.query = Objects.requireNonNull(query, "query");
    Objects.requireNonNull(spec, "spec");
    this.filtering = new Filtering(spec);
  }

  @Override
  public SelenideTableQuery<C> query() {
    return query;
  }

  @Override
  public TableFiltering<C> filtering() {
    return filtering;
  }

  private final class Filtering implements TableFiltering<C> {
    private final TableFilteringSpec<C> spec;

    private Filtering(TableFilteringSpec<C> spec) {
      this.spec = spec;
    }

    @Override
    public FilterableTable<C> set(C column, String value) {
      return set(column, value, TableCapabilityStateWaiter.defaultTimeout());
    }

    @Override
    public FilterableTable<C> set(C column, String value, Duration timeout) {
      C requiredColumn = Objects.requireNonNull(column, "column");
      Objects.requireNonNull(value, "value");
      TableCapabilityStateWaiter.perform(
          "filtering by " + requiredColumn + "=" + value,
          spec.stateToken(),
          () -> spec.control().apply(requiredColumn).setValue(value),
          timeout);
      return SelenideFilterableTable.this;
    }

    @Override
    public FilterableTable<C> clear(C column) {
      return clear(column, TableCapabilityStateWaiter.defaultTimeout());
    }

    @Override
    public FilterableTable<C> clear(C column, Duration timeout) {
      C requiredColumn = Objects.requireNonNull(column, "column");
      TableCapabilityStateWaiter.perform(
          "clearing filter for " + requiredColumn,
          spec.stateToken(),
          () -> spec.control().apply(requiredColumn).clear(),
          timeout);
      return SelenideFilterableTable.this;
    }
  }
}
