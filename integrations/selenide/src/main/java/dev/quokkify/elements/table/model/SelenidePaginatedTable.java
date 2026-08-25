package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

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
      return isEnabled(spec.nextControl().get());
    }

    @Override
    public boolean canPrevious() {
      return isEnabled(spec.previousControl().get());
    }

    @Override
    public Optional<TablePageMetadata> metadata() {
      return spec.metadata().get();
    }

    private boolean isEnabled(SelenideElement control) {
      String ariaDisabled = control.getAttribute("aria-disabled");
      return !control.is(Condition.disabled) && !"true".equalsIgnoreCase(ariaDisabled);
    }
  }
}
