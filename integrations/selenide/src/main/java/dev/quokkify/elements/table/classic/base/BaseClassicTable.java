package dev.quokkify.elements.table.classic.base;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import dev.quokkify.elements.base.BaseTable;
import dev.quokkify.elements.table.classic.Row;
import dev.quokkify.ex.TableRowException;
import dev.quokkify.html.model.HtmlTag;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.impl.WebElementWrapper;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.Configuration.timeout;

/**
 * Abstract class to work with Classic Table.
 *
 * @param <T> enum with columns enumerations
 */
public abstract class BaseClassicTable<T extends Enum<T>> extends BaseTable<T> {

  /**
   * Get row by values in given columns, waiting for it to appear with the default timeout.
   *
   * @param expectedRowValues map: key - column, value - cell value
   * @return table {@link Row} element
   */
  public Row<T> getRow(Map<T, String> expectedRowValues) {
    return this.getRow(expectedRowValues, Duration.ofMillis(timeout));
  }

  /**
   * Get row by values in given columns, waiting for it to appear.
   *
   * @param expectedRowValues map: key - column, value - cell value
   * @param timeout           how long to wait for a matching row
   * @return table {@link Row} element
   */
  public Row<T> getRow(Map<T, String> expectedRowValues, Duration timeout) {
    return this.getFilteredRow(this.columnsTextsPredicate(expectedRowValues),
        "row with expected values %s".formatted(expectedRowValues),
        cause -> new TableRowException(expectedRowValues, timeout, cause), timeout);
  }

  /**
   * Get row by value in given column, waiting for it to appear with the default timeout.
   *
   * @param columnHeader column enum
   * @param cellValue    expected cell value
   * @return table {@link Row} element
   */
  public Row<T> getRow(T columnHeader, String cellValue) {
    return this.getRow(columnHeader, cellValue, Duration.ofMillis(timeout));
  }

  /**
   * Get row by value in given column, waiting for it to appear.
   *
   * @param columnHeader    column enum
   * @param cellValue       expected cell value
   * @param timeout         how long to wait for a matching row
   * @return table {@link Row} element
   */
  public Row<T> getRow(T columnHeader, String cellValue, Duration timeout) {
    return this.getFilteredRow(this.columnTextPredicate(columnHeader, cellValue),
        "row with '%s' in '%s' column".formatted(cellValue, columnHeader),
        cause -> new TableRowException(columnHeader, cellValue, timeout, cause), timeout);
  }

  /**
   * Get row by pattern in given column, waiting for it to appear with the default timeout.
   *
   * @param columnHeader column enum
   * @param pattern      expected cell pattern
   * @return table {@link Row} element
   */
  public Row<T> getRowByPattern(T columnHeader, String pattern) {
    return this.getRowByPattern(columnHeader, pattern, Duration.ofMillis(timeout));
  }

  /**
   * Get row by pattern in given column, waiting for it to appear.
   *
   * @param columnHeader    column enum
   * @param pattern         expected cell pattern
   * @param timeout         how long to wait for a matching row
   * @return table {@link Row} element
   */
  public Row<T> getRowByPattern(T columnHeader, String pattern, Duration timeout) {
    return this.getFilteredRow(this.columnTextPredicateByPattern(columnHeader, pattern),
        "row matching '%s' in '%s' column".formatted(pattern, columnHeader),
        cause -> new TableRowException(columnHeader, pattern, timeout, cause), timeout);
  }

  /**
   * Get existing row status by values in columns.
   *
   * @param expectedRowValues map: key - column, value - cell value
   * @return true if row contains all values, otherwise false
   */
  public boolean isRowExist(Map<T, String> expectedRowValues) {
    return isRowExist(columnsTextsPredicate(expectedRowValues));
  }

  /**
   * Get existing row status by value in column.
   *
   * @param columnHeader column enum
   * @param cellValue    expected cell value
   * @return true if row value equals expected value, otherwise false
   */
  public boolean isRowExist(T columnHeader, String cellValue) {
    return isRowExist(columnTextPredicate(columnHeader, cellValue));
  }

  /**
   * Get existing row status.
   *
   * @param condition how to filter all rows in the table
   * @return boolean row existing status
   */
  private boolean isRowExist(Predicate<Row<T>> condition) {
    try {
      return getAllRows().stream().anyMatch(condition);
    } catch (NoSuchElementException | StaleElementReferenceException | UIAssertionError error) {
      return false;
    }
  }

  /**
   * Get a filtered row by the given condition, waiting through Selenide's native condition loop.
   * Kept for source compatibility with subclasses extending this class.
   *
   * @param condition         condition how to filter all rows in the table
   * @param tableRowException error if no suitable row appears within Selenide's timeout
   * @return filtered row
   */
  protected Row<T> getFilteredRow(Predicate<Row<T>> condition, Supplier<TableRowException> tableRowException) {
    return getFilteredRow(condition, "matching table row", cause -> tableRowException.get(),
        Duration.ofMillis(timeout));
  }

  /**
   * Wait for a row with Selenide's own retry mechanism. The timeout is per call while the polling
   * interval comes from Selenide configuration, so there is one authoritative wait loop.
   */
  protected Row<T> getFilteredRow(Predicate<Row<T>> condition, String description,
                                  Function<Throwable, TableRowException> tableRowException,
                                  Duration rowTimeout) {
    MatchingRowCondition matchingRow = new MatchingRowCondition(description, condition);
    try {
      getSelf().shouldHave(matchingRow, rowTimeout);
      return matchingRow.matchedRow();
    } catch (UIAssertionError error) {
      throw tableRowException.apply(error);
    }
  }

  private final class MatchingRowCondition extends WebElementCondition {

    private final Predicate<Row<T>> condition;
    private SelenideElement matchedElement;

    private MatchingRowCondition(String description, Predicate<Row<T>> condition) {
      super(description);
      this.condition = condition;
    }

    @Override
    public CheckResult check(Driver driver, WebElement table) {
      try {
        Map<T, Integer> columnIndexes = new HashMap<>();
        Function<T, Integer> columnIndexResolver = column ->
            columnIndexes.computeIfAbsent(column, key -> fetchColumnIndex(driver, table, key));
        List<WebElement> rowElements = getAllRowsElements(table);
        for (WebElement rowElement : rowElements) {
          SelenideElement candidateElement = WebElementWrapper.wrap(driver, rowElement, "table row");
          if (condition.test(mapToRow(candidateElement, columnIndexResolver))) {
            matchedElement = candidateElement;
            return CheckResult.accepted("matched row: " + rowElement.getText());
          }
        }
        matchedElement = null;
        return CheckResult.rejected("No matching row yet", "rows checked: " + rowElements.size());
      } catch (NoSuchElementException | StaleElementReferenceException error) {
        matchedElement = null;
        return CheckResult.rejected(error.toString(), "table changed while checking rows");
      }
    }

    private Row<T> matchedRow() {
      if (matchedElement == null) {
        throw new IllegalStateException("Selenide accepted the row condition without a matched row");
      }
      return mapToRow(matchedElement, fetchColumnIndex());
    }
  }

  /**
   * Get row predicate by expected row values.
   *
   * @param expectedRowValues {@link Map} expected row values
   * @return table {@link Row} element
   */
  private Predicate<Row<T>> columnsTextsPredicate(Map<T, String> expectedRowValues) {
    return row -> expectedRowValues.entrySet().stream()
        .allMatch(entry ->
            row.getCell(entry.getKey()).getSelf().has(Condition.exactTextCaseSensitive(entry.getValue())));
  }

  /**
   * Get row predicate by cell value.
   *
   * @param columnHeader column enum
   * @param cellValue    expected cell value
   * @return table {@link Row} element
   */
  private Predicate<Row<T>> columnTextPredicate(T columnHeader, String cellValue) {
    return row -> row.getCell(columnHeader).getSelf().has(Condition.exactTextCaseSensitive(cellValue));
  }

  /**
   * Get row predicate by cell pattern.
   *
   * @param columnHeader column enum
   * @param pattern      expected cell pattern
   * @return table {@link Row} element
   */
  private Predicate<Row<T>> columnTextPredicateByPattern(T columnHeader, String pattern) {
    return row -> row.getCell(columnHeader).getSelf().getText().matches(pattern);
  }

  /**
   * Get all column values.
   *
   * @param columnHeader column enum
   * @param tag          to specify the html-tag from a cell to get only one value from all cells,
   * @return list of column values
   */
  public List<String> getAllColumnValuesByTag(T columnHeader, String tag) {
    return getAllColumnValuesByXpath(columnHeader, "//%s".formatted(tag));
  }

  /**
   * Get all column values.
   *
   * @param columnHeader column enum
   * @return list of column values
   */
  public List<String> getAllColumnValuesByXpath(T columnHeader) {
    return getAllColumnValuesByXpath(columnHeader, StringUtils.EMPTY);
  }

  /**
   * Get all column values.
   *
   * @param columnHeader           column enum
   * @param xpathAdditionalLocator additional xPath locator to get a specific element in a cell
   * @return list of column values
   */
  public List<String> getAllColumnValuesByXpath(T columnHeader, String xpathAdditionalLocator) {
    return getSelf().findAll(By.xpath(".//%s[%d]%s".formatted(
        HtmlTag.TD,
            fetchColumnIndex().apply(columnHeader) + HTML_START_INDEX,
            xpathAdditionalLocator)
        ))
        .texts();
  }

  /**
   * Get first row from table.
   *
   * @return table {@link Row} element
   */
  @Override
  public Row<T> getFirstRow() {
    return getAllRows().stream()
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No rows found"));
  }

  /**
   * Get all table rows.
   *
   * @return all table {@link Row} element
   */
  @Override
  public List<Row<T>> getAllRows() {
    return getAllRowsElements().asFixedIterable().stream()
        .map(this::mapToRow).collect(Collectors.toList());
  }

  /**
   * Map element to row object.
   *
   * @param element {@link SelenideElement}
   * @return table {@link Row} element
   */
  protected Row<T> mapToRow(SelenideElement element) {
    return mapToRow(element, fetchColumnIndex());
  }

  /**
   * Map element to row object using the given column-index resolver.
   *
   * @param element             {@link SelenideElement}
   * @param columnIndexResolver column-index lookup for cells of the mapped row
   * @return table {@link Row} element
   */
  protected Row<T> mapToRow(SelenideElement element, Function<T, Integer> columnIndexResolver) {
    return new Row<>(element, columnIndexResolver);
  }

  /**
   * Get all rows as elements collection.
   *
   * @return {@link ElementsCollection}
   */
  protected ElementsCollection getAllRowsElements() {
    ElementsCollection rowsWithHeader = this.getSelf().findAll(By.tagName(HtmlTag.TR));
    return rowsWithHeader.last(Math.max(0, rowsWithHeader.size() - HTML_START_INDEX));
  }

  /**
   * Read rows from the root element already supplied to a native Selenide condition.
   */
  protected List<WebElement> getAllRowsElements(WebElement table) {
    List<WebElement> rowsWithHeader = table.findElements(By.tagName(HtmlTag.TR));
    return rowsWithHeader.size() <= HTML_START_INDEX
        ? List.of()
        : rowsWithHeader.subList(HTML_START_INDEX, rowsWithHeader.size());
  }
}
