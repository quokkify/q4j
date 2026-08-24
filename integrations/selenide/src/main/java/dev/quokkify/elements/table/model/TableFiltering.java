package dev.quokkify.elements.table.model;

import java.time.Duration;

/** Filtering actions for an opt-in table capability. */
public interface TableFiltering<C> {

  FilterableTable<C> set(C column, String value);

  FilterableTable<C> set(C column, String value, Duration timeout);

  FilterableTable<C> clear(C column);

  FilterableTable<C> clear(C column, Duration timeout);
}
