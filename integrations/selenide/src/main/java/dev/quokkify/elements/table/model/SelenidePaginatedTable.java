package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class SelenidePaginatedTable<C> implements PaginatedTable<C> {

  private final SelenideTableQuery<C> query;
  private final TablePagination<C> pagination;

  public SelenidePaginatedTable(SelenideTableQuery<C> query, TablePaginationSpec spec) {
    this.query = Objects.requireNonNull(query, "query");
    Objects.requireNonNull(spec, "spec");
    this.pagination = new Pagination(spec);
  }

  @Override
  public SelenideTableQuery<C> query() {
    return query;
  }

  @Override
  public TablePagination<C> pagination() {
    return pagination;
  }

  private final class Pagination implements TablePagination<C> {
    private final TablePaginationSpec spec;

    private Pagination(TablePaginationSpec spec) {
      this.spec = spec;
    }

    @Override
    public PaginatedTable<C> nextPage() {
      return nextPage(TableCapabilityStateWaiter.defaultTimeout());
    }

    @Override
    public PaginatedTable<C> nextPage(Duration timeout) {
      if (!canNext()) {
        throw new IllegalStateException("Next page is not available");
      }
      TableCapabilityStateWaiter.perform("moving to next page", spec.stateToken(),
          () -> spec.nextControl().get().click(), timeout);
      return SelenidePaginatedTable.this;
    }

    @Override
    public PaginatedTable<C> previousPage() {
      return previousPage(TableCapabilityStateWaiter.defaultTimeout());
    }

    @Override
    public PaginatedTable<C> previousPage(Duration timeout) {
      if (!canPrevious()) {
        throw new IllegalStateException("Previous page is not available");
      }
      TableCapabilityStateWaiter.perform("moving to previous page", spec.stateToken(),
          () -> spec.previousControl().get().click(), timeout);
      return SelenidePaginatedTable.this;
    }

    @Override
    public boolean canNext() {
      return spec.canNext().getAsBoolean();
    }

    @Override
    public boolean canPrevious() {
      return spec.canPrevious().getAsBoolean();
    }

    @Override
    public Optional<TablePageMetadata> metadata() {
      return spec.metadata().get();
    }
  }
}
