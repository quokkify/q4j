package dev.quokkify.elements.table;

import java.util.Objects;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;

/**
 * Thin header-aware view over a table-like element. All lookups are lazy Selenide collections and elements.
 */
public final class Table {

  private static final String UNSUPPORTED_COLUMN =
      "column() needs XPath row and single-step cell locators; use rows() and TableRow.cell()";

  private final SelenideElement root;
  private final TableLayout layout;

  private Table(SelenideElement root, TableLayout layout) {
    this.root = Objects.requireNonNull(root, "root");
    this.layout = Objects.requireNonNull(layout, "layout");
  }

  public static Table of(SelenideElement root, TableLayout layout) {
    return new Table(root, layout);
  }

  public SelenideElement root() {
    return root;
  }

  public ElementsCollection headers() {
    return root.$$(layout.headers());
  }

  public ElementsCollection rows() {
    return root.$$(layout.rows());
  }

  public TableRow row(int index) {
    return new TableRow(rows().get(index), this);
  }

  public TableRow row(String column, String value) {
    return new TableRow(rows().findBy(new ColumnValueCondition(this, column, value)), this);
  }

  public ElementsCollection rows(String column, String value) {
    return rows().filterBy(new ColumnValueCondition(this, column, value));
  }

  /**
   * Cells of the given column in every row. Supported only when the layout's rows locator is {@code By.xpath} and
   * its cells locator is a single {@code By.xpath} child step starting with {@code ./} (no {@code |} unions), as in
   * {@link TableLayout#html()} and {@link TableLayout#aria()}; other layouts throw
   * {@link UnsupportedOperationException}, use {@link #rows()} and {@link TableRow#cell(String)} instead.
   * The column index is resolved once, when this method is called.
   */
  public ElementsCollection column(String header) {
    String rowsXpath = TableLayout.xpath(layout.rows()).orElse(null);
    String cellsXpath = TableLayout.xpath(layout.cells()).orElse(null);
    if (rowsXpath == null || cellsXpath == null || !cellsXpath.startsWith("./") || cellsXpath.contains("|")) {
      throw new UnsupportedOperationException(UNSUPPORTED_COLUMN);
    }
    int index = columnIndex(header);
    return root.$$(By.xpath(rowsXpath + "/" + cellsXpath.substring(2) + "[" + (index + 1) + "]"));
  }

  TableLayout layout() {
    return layout;
  }

  int columnIndex(String header) {
    ElementsCollection headers = headers();
    headers.shouldHave(sizeGreaterThan(0));
    return ColumnResolver.indexOf(headers.texts(), header, root.toString());
  }
}
