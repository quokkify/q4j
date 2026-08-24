package dev.quokkify.elements.table.model;

/** Opt-in sorting capability composed around a table query. */
public interface SortableTable<C> {

  SelenideTableQuery<C> query();

  TableSorting<C> sorting();
}
