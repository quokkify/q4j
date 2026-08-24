package dev.quokkify.elements.table.model;

/** Opt-in pagination capability composed around a table query. */
public interface PaginatedTable<C> {

  SelenideTableQuery<C> query();

  TablePagination<C> pagination();
}
