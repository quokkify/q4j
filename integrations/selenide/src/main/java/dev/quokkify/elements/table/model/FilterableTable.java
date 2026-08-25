package dev.quokkify.elements.table.model;

/** Opt-in filtering capability composed around a table query. */
public interface FilterableTable<C, F> {

  SelenideTableQuery<C> query();

  TableFiltering<F> filtering();
}
