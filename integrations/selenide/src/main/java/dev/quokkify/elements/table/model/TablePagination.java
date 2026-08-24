package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.Optional;

/** Pagination actions and metadata for an opt-in table capability. */
public interface TablePagination<C> {

  PaginatedTable<C> nextPage();

  PaginatedTable<C> nextPage(Duration timeout);

  PaginatedTable<C> previousPage();

  PaginatedTable<C> previousPage(Duration timeout);

  boolean canNext();

  boolean canPrevious();

  Optional<TablePageMetadata> metadata();
}
