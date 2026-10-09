package dev.quokkify.elements.table;

import java.util.Objects;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.UIAssertionError;
import org.openqa.selenium.By;

import static com.codeborne.selenide.CollectionCondition.anyMatch;
import static com.codeborne.selenide.CollectionCondition.itemWithText;

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

  /**
   * Wraps a row element found by the caller, so its cells can be addressed by header. The element must be a row
   * of this table matching the layout's cells locator; it may be lazy.
   */
  public TableRow row(SelenideElement row) {
    return new TableRow(Objects.requireNonNull(row, "row"), this);
  }

  public ElementsCollection rows(String column, String value) {
    return rows().filterBy(new ColumnValueCondition(this, column, value));
  }

  /**
   * Cells of the given column in every row. Supported only when the layout's rows locator is {@code By.xpath} and
   * its cells locator is a single {@code By.xpath} child step starting with {@code ./} (not {@code .//}, no
   * {@code |} unions), as in {@link TableLayout#html()} and {@link TableLayout#aria()}; other layouts throw
   * {@link UnsupportedOperationException}, use {@link #rows()} and {@link TableRow#cell(String)} instead.
   * The column index is resolved once, when this method is called.
   */
  public ElementsCollection column(String header) {
    String rowsXpath = TableLayout.xpath(layout.rows()).orElse(null);
    String cellStep = cellStep(rowsXpath, TableLayout.xpath(layout.cells()).orElse(null));
    return root.$$(By.xpath(columnXpath(rowsXpath, cellStep, columnIndex(header))));
  }

  static String cellStep(String rowsXpath, String cellsXpath) {
    if (rowsXpath == null || cellsXpath == null || !cellsXpath.startsWith("./") || cellsXpath.startsWith(".//")
        || cellsXpath.contains("|")) {
      throw new UnsupportedOperationException(UNSUPPORTED_COLUMN);
    }
    return cellsXpath.substring(2);
  }

  static String columnXpath(String rowsXpath, String cellStep, int index) {
    return "(" + rowsXpath + ")/" + cellStep + "[" + (index + 1) + "]";
  }

  TableLayout layout() {
    return layout;
  }

  int columnIndex(String header) {
    ElementsCollection headers = headers();
    String expected = ColumnResolver.normalize(header);
    try {
      headers.shouldHave(itemWithText(header).or(anyMatch("normalized header \"" + expected + "\"",
          th -> ColumnResolver.normalize(th.getText()).equals(expected))));
    } catch (UIAssertionError e) {
      throw new TableColumnException(header, "not found", root.toString(), ColumnResolver.normalize(headers.texts()));
    }
    return ColumnResolver.indexOf(headers.texts(), header, root.toString());
  }
}
