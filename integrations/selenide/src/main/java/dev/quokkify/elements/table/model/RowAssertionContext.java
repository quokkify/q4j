package dev.quokkify.elements.table.model;

import java.util.List;

import org.openqa.selenium.WebElement;

/** Current row DOM snapshot used only during one Selenide condition evaluation. */
public final class RowAssertionContext<C> {

  private final int index;
  private final WebElement row;
  private final TableDomAdapter adapter;
  private final TableAssertionContext<C> table;

  RowAssertionContext(int index, WebElement row, TableDomAdapter adapter,
                      TableAssertionContext<C> table) {
    this.index = index;
    this.row = row;
    this.adapter = adapter;
    this.table = table;
  }

  public int index() {
    return index;
  }

  public List<String> values() {
    return cells().stream().map(WebElement::getText).toList();
  }

  WebElement cell(int columnIndex) {
    return cells().get(columnIndex);
  }

  int columnIndex(C column) {
    if (adapter.headerLocator() instanceof RowHeaderCellLocator) {
      int headerIndex = table.columnIndex(column);
      return headerIndex == index ? 0 : -1;
    }
    return table.columnIndex(column);
  }

  private List<WebElement> cells() {
    return row.findElements(adapter.dataCellLocator());
  }
}
