package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.Objects;

public final class SelenideFilterableTable<C, F> implements FilterableTable<C, F> {

  private final SelenideTableQuery<C> query;
  private final TableFiltering<F> filtering;

  public SelenideFilterableTable(SelenideTableQuery<C> query, TableFilteringSpec<F> spec) {
    this.query = Objects.requireNonNull(query, "query");
    Objects.requireNonNull(spec, "spec");
    this.filtering = new Filtering(spec);
  }

  @Override
  public SelenideTableQuery<C> query() {
    return query;
  }

  @Override
  public TableFiltering<F> filtering() {
    return filtering;
  }

  private final class Filtering implements TableFiltering<F> {
    private final TableFilteringSpec<F> spec;

    private Filtering(TableFilteringSpec<F> spec) {
      this.spec = spec;
    }

    @Override
    public FilterableTable<C, F> set(F column, String value) {
      return set(column, value, TableCapabilityStateWaiter.defaultTimeout());
    }

    @Override
    public FilterableTable<C, F> set(F column, String value, Duration timeout) {
      F requiredFilter = Objects.requireNonNull(column, "filter");
      Objects.requireNonNull(value, "value");
      TableCapabilityStateWaiter.perform(
          "filtering by " + requiredFilter + "=" + value,
          spec.stateToken(),
          () -> spec.control().apply(requiredFilter).setValue(value),
          timeout);
      return SelenideFilterableTable.this;
    }

    @Override
    public FilterableTable<C, F> clear(F column) {
      return clear(column, TableCapabilityStateWaiter.defaultTimeout());
    }

    @Override
    public FilterableTable<C, F> clear(F column, Duration timeout) {
      F requiredFilter = Objects.requireNonNull(column, "filter");
      TableCapabilityStateWaiter.perform(
          "clearing filter for " + requiredFilter,
          spec.stateToken(),
          () -> spec.control().apply(requiredFilter).clear(),
          timeout);
      return SelenideFilterableTable.this;
    }

    @Override
    public java.util.Optional<String> currentValue(F filter) {
      return spec.currentValue().apply(Objects.requireNonNull(filter, "filter"));
    }
  }
}
