package dev.quokkify.elements.table;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

/**
 * A row of a {@link Table}; cells are addressed by index or by displayed header text.
 */
public final class TableRow {

  private final SelenideElement self;
  private final Table table;

  TableRow(SelenideElement self, Table table) {
    this.self = self;
    this.table = table;
  }

  public SelenideElement self() {
    return self;
  }

  public ElementsCollection cells() {
    return self.$$(table.layout().cells());
  }

  public SelenideElement cell(int index) {
    return cells().get(index);
  }

  public SelenideElement cell(String header) {
    return cells().get(table.columnIndex(header));
  }
}
