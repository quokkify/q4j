package dev.quokkify.elements.table;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.UIAssertionError;
import org.openqa.selenium.By;

import static com.codeborne.selenide.CollectionCondition.anyMatch;
import static com.codeborne.selenide.CollectionCondition.itemWithText;

/**
 * Thin header-aware view over a table-like element. Returned elements and collections are lazy:
 * {@code row(column, value)} and {@code rows(column, value)} re-resolve the column on every evaluation, while
 * {@code cell(header)} and {@code column(header)} resolve the column index once, when called (call them again after
 * columns are reordered).
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
   * Reads headers and every row's cell texts in one browser call. The result is plain data read at this moment: it
   * waits only for the root to exist (Selenide's default timeout), not for rows. To verify data, prefer
   * {@link #shouldHave(TableCondition)} or {@link #shouldSatisfy(Consumer)}, which re-read until the check passes.
   * Supports {@code By.xpath} and {@code By.cssSelector} layouts.
   */
  public TableSnapshot snapshot() {
    return read(null);
  }

  /**
   * Like {@link #snapshot()}, and also reads the texts of the elements matching {@code items} inside each cell (for
   * example chips or tags) into {@link SnapshotCell#items()}. Hidden items are skipped.
   */
  public TableSnapshot snapshot(By items) {
    return read(SnapshotLocator.of(Objects.requireNonNull(items, "items")));
  }

  private TableSnapshot read(SnapshotLocator items) {
    SnapshotCondition read = condition("be readable as a table", items, snapshot -> CheckResult.accepted());
    root.should(read);
    TableSnapshot snapshot = read.last();
    if (Objects.isNull(snapshot)) {
      // AssertionMode.SOFT collects a failed should() and returns without a read; do not hand back null.
      throw new IllegalStateException("table " + root + " was not read; see the collected root failure");
    }
    return snapshot;
  }

  /**
   * Waits until the table satisfies {@code condition}, with Selenide's default timeout. Each attempt reads the table
   * in one browser call, like {@link #snapshot()}, so the cost does not grow with the number of rows; a failure is a
   * Selenide {@code ElementShould} on the root whose actual value describes the difference.
   */
  public Table shouldHave(TableCondition condition) {
    root.shouldHave(condition(condition.toString(), condition.items(), condition::evaluate));
    return this;
  }

  /** Like {@link #shouldHave(TableCondition)}, with an explicit timeout. */
  public Table shouldHave(TableCondition condition, Duration timeout) {
    root.shouldHave(condition(condition.toString(), condition.items(), condition::evaluate), timeout);
    return this;
  }

  /**
   * Waits until {@code assertions} pass on a fresh {@link #snapshot()}, with Selenide's default timeout. Each attempt
   * reads the whole table in one browser call, so a table that is still rendering or being remounted is simply read
   * again. A failed assertion, a header not displayed yet or ambiguous, a row or cell not rendered yet and a key column
   * with a missing cell or repeated key are retried; on timeout it fails like any Selenide {@code should*}, with the
   * last failure as the actual value. Any other exception thrown by {@code assertions} fails at once, without retry.
   */
  public Table shouldSatisfy(Consumer<TableSnapshot> assertions) {
    root.should(satisfying(null, assertions));
    return this;
  }

  /** Like {@link #shouldSatisfy(Consumer)}, with an explicit timeout. */
  public Table shouldSatisfy(Consumer<TableSnapshot> assertions, Duration timeout) {
    root.should(satisfying(null, assertions), timeout);
    return this;
  }

  /** Like {@link #shouldSatisfy(Consumer)}, over a snapshot that also reads {@code items} inside each cell. */
  public Table shouldSatisfy(By items, Consumer<TableSnapshot> assertions) {
    root.should(satisfying(SnapshotLocator.of(Objects.requireNonNull(items, "items")), assertions));
    return this;
  }

  /** Like {@link #shouldSatisfy(By, Consumer)}, with an explicit timeout. */
  public Table shouldSatisfy(By items, Consumer<TableSnapshot> assertions, Duration timeout) {
    root.should(satisfying(SnapshotLocator.of(Objects.requireNonNull(items, "items")), assertions), timeout);
    return this;
  }

  private SnapshotCondition satisfying(SnapshotLocator items, Consumer<TableSnapshot> assertions) {
    Objects.requireNonNull(assertions, "assertions");
    return condition("table data satisfying the assertions", items,
        snapshot -> SnapshotCondition.evaluate(snapshot, assertions));
  }

  /**
   * A condition reading this table with the script locators of rows, cells, headers and {@code items}; rejects a
   * locator other than XPath or CSS with {@link UnsupportedOperationException} before any browser call.
   */
  private SnapshotCondition condition(String description, SnapshotLocator items,
      Function<TableSnapshot, CheckResult> judge) {
    List<List<String>> locators = List.of(SnapshotLocator.of(layout.rows()).toScriptArg(),
        SnapshotLocator.of(layout.cells()).toScriptArg(), SnapshotLocator.of(layout.headers()).toScriptArg(),
        Objects.isNull(items) ? List.of() : items.toScriptArg());
    return new SnapshotCondition(description, root.toString(), locators, judge);
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
        || cellsXpath.contains("|") || isMultiStep(cellsXpath.substring(2))) {
      throw new UnsupportedOperationException(UNSUPPORTED_COLUMN);
    }
    return cellsXpath.substring(2);
  }

  private static boolean isMultiStep(String step) {
    int depth = 0;
    char quote = 0;
    for (char c : step.toCharArray()) {
      if (quote != 0) {
        if (c == quote) {
          quote = 0;
        }
      } else if (c == '\'' || c == '"') {
        quote = c;
      } else if (c == '[' || c == '(') {
        depth++;
      } else if (c == ']' || c == ')') {
        depth--;
      } else if (c == '/' && depth == 0) {
        return true;
      }
    }
    return false;
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
