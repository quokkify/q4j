package dev.quokkify.elements.table.model;

import java.time.Duration;

/** Filtering actions for an opt-in table capability. */
public interface TableFiltering<F> {

  FilterableTable<?, F> set(F filter, String value);

  FilterableTable<?, F> set(F filter, String value, Duration timeout);

  FilterableTable<?, F> clear(F filter);

  FilterableTable<?, F> clear(F filter, Duration timeout);

  java.util.Optional<String> currentValue(F filter);
}
