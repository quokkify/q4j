package dev.quokkify.elements.table.model;

import java.util.List;
import java.util.Objects;

import org.openqa.selenium.WebElement;

/** Current table DOM snapshot used only during one Selenide condition evaluation. */
public final class TableAssertionContext<C> {

  private final WebElement table;
  private final TableDomAdapter adapter;
  private final DisplayedHeaderResolver<C> resolver;
  private final List<WebElement> rows;
  private final List<String> headers;

  TableAssertionContext(WebElement table, TableDomAdapter adapter,
                        DisplayedHeaderResolver<C> resolver) {
    this.table = Objects.requireNonNull(table, "table");
    this.adapter = Objects.requireNonNull(adapter, "adapter");
    this.resolver = Objects.requireNonNull(resolver, "resolver");
    this.rows = List.copyOf(table.findElements(adapter.mountedDataRowLocator()));
    this.headers = readHeaders();
  }

  public List<String> headers() {
    return headers;
  }

  public int rowCount() {
    return rows.size();
  }

  public List<RowAssertionContext<C>> rows() {
    return java.util.stream.IntStream.range(0, rows.size())
        .mapToObj(this::row).toList();
  }

  public RowAssertionContext<C> row(int index) {
    return new RowAssertionContext<>(index, rows.get(index), adapter, this);
  }

  public int columnIndex(C column) {
    String displayed = resolver.displayedHeader(Objects.requireNonNull(column, "column"));
    int first = headers.indexOf(displayed);
    if (first < 0) {
      return -1;
    }
    return headers.lastIndexOf(displayed) == first ? first : -2;
  }

  public List<String> columnValues(int index) {
    if (adapter.headerLocator() instanceof RowHeaderCellLocator) {
      return index >= 0 && index < rows.size() ? row(index).values() : List.of();
    }
    return rows().stream().filter(row -> index < row.values().size())
        .map(row -> row.values().get(index)).toList();
  }

  public String diagnostics() {
    return "headers=" + headers + ", rows=" + rows().stream()
        .map(RowAssertionContext::values).toList();
  }

  private List<String> readHeaders() {
    if (adapter.headerLocator() instanceof TableHeaderRowLocator header) {
      List<WebElement> headerRows = table.findElements(header.headerRowLocator());
      return headerRows.isEmpty() ? List.of()
          : headerRows.get(0).findElements(header.headerCellLocator()).stream()
              .map(WebElement::getText).toList();
    }
    if (adapter.headerLocator() instanceof RowHeaderCellLocator header) {
      return rows.stream().map(row -> row.findElement(header.headerCellLocator()).getText()).toList();
    }
    return List.of();
  }
}
