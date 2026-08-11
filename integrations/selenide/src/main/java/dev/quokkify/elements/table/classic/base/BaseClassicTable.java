package dev.quokkify.elements.table.classic.base;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import dev.quokkify.elements.base.BaseTable;
import dev.quokkify.elements.table.classic.Row;
import dev.quokkify.ex.TableRowException;
import dev.quokkify.html.model.HtmlTag;
import dev.quokkify.util.Waiter;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.UIAssertionError;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;

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
    return this.getRow(expectedRowValues, DEFAULT_ROW_TIMEOUT, DEFAULT_ROW_POLLING_INTERVAL);
  }

  /**
   * Get row by values in given columns, waiting for it to appear.
   *
   * @param expectedRowValues map: key - column, value - cell value
   * @param timeout           how long to wait for a matching row
   * @param pollingInterval   how often to re-read the table rows
   * @return table {@link Row} element
   */
  public Row<T> getRow(Map<T, String> expectedRowValues, Duration timeout, Duration pollingInterval) {
    return this.getFilteredRow(this.columnsTextsPredicate(expectedRowValues),
        cause -> new TableRowException(expectedRowValues, timeout, cause), timeout, pollingInterval);
  }

  /**
   * Get row by value in given column, waiting for it to appear with the default timeout.
   *
   * @param columnHeader column enum
   * @param cellValue    expected cell value
   * @return table {@link Row} element
   */
  public Row<T> getRow(T columnHeader, String cellValue) {
    return this.getRow(columnHeader, cellValue, DEFAULT_ROW_TIMEOUT, DEFAULT_ROW_POLLING_INTERVAL);
  }

  /**
   * Get row by value in given column, waiting for it to appear.
   *
   * @param columnHeader    column enum
   * @param cellValue       expected cell value
   * @param timeout         how long to wait for a matching row
   * @param pollingInterval how often to re-read the table rows
   * @return table {@link Row} element
   */
  public Row<T> getRow(T columnHeader, String cellValue, Duration timeout, Duration pollingInterval) {
    return this.getFilteredRow(this.columnTextPredicate(columnHeader, cellValue),
        cause -> new TableRowException(columnHeader, cellValue, timeout, cause), timeout, pollingInterval);
  }

  /**
   * Get row by pattern in given column, waiting for it to appear with the default timeout.
   *
   * @param columnHeader column enum
   * @param pattern      expected cell pattern
   * @return table {@link Row} element
   */
  public Row<T> getRowByPattern(T columnHeader, String pattern) {
    return this.getRowByPattern(columnHeader, pattern, DEFAULT_ROW_TIMEOUT, DEFAULT_ROW_POLLING_INTERVAL);
  }

  /**
   * Get row by pattern in given column, waiting for it to appear.
   *
   * @param columnHeader    column enum
   * @param pattern         expected cell pattern
   * @param timeout         how long to wait for a matching row
   * @param pollingInterval how often to re-read the table rows
   * @return table {@link Row} element
   */
  public Row<T> getRowByPattern(T columnHeader, String pattern, Duration timeout, Duration pollingInterval) {
    return this.getFilteredRow(this.columnTextPredicateByPattern(columnHeader, pattern),
        cause -> new TableRowException(columnHeader, pattern, timeout, cause), timeout, pollingInterval);
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
    return getAllRows().stream().anyMatch(condition);
  }

  /**
   * Get a filtered row by the given condition, waiting until a matching row appears, using a
   * legacy row-not-found error factory that does not carry the {@link ConditionTimeoutException}
   * cause. Kept for binary/source compatibility with subclasses extending this class.
   *
   * @param condition         condition how to filter all rows in the table
   * @param tableRowException error if no suitable row appears in the table within the timeout
   * @return filtered row as {@link SelenideElement}
   */
  protected Row<T> getFilteredRow(Predicate<Row<T>> condition, Supplier<TableRowException> tableRowException) {
    return getFilteredRow(condition, cause -> tableRowException.get(), DEFAULT_ROW_TIMEOUT,
        DEFAULT_ROW_POLLING_INTERVAL);
  }

  /**
   * Get a filtered row by the given condition, waiting until a matching row appears.
   *
   * @param condition         condition how to filter all rows in the table
   * @param tableRowException error if no suitable row appears in the table within the timeout,
   *                          receiving the {@link ConditionTimeoutException} that caused the wait to give up
   * @param timeout           how long to wait for a matching row
   * @param pollingInterval   how often to re-read the table rows
   * @return filtered row as {@link SelenideElement}
   */
  protected Row<T> getFilteredRow(Predicate<Row<T>> condition,
                                  Function<Throwable, TableRowException> tableRowException,
                                  Duration timeout, Duration pollingInterval) {
    Function<T, Integer> columnIndexResolver = memoizeColumnIndex();
    try {
      return Waiter.awaitCondition(() -> findFirstMatch(columnIndexResolver, condition),
          Matchers.notNullValue(), "Waiting for matching table row", timeout, pollingInterval);
    } catch (ConditionTimeoutException e) {
      throw tableRowException.apply(e);
    }
  }

  /**
   * Resolve one row matching the given condition, tolerating the transient exceptions that can be
   * thrown while the table (or its rows) is still mounting asynchronously, so the caller's poll
   * loop keeps retrying instead of aborting on the first failed read.
   *
   * <p>The matched row is re-wrapped with a fresh (non-memoized) column-index resolver before
   * being returned, so callers of the returned {@link Row} keep re-reading column positions from
   * the live DOM afterwards instead of being pinned to the index snapshot taken during the wait.
   *
   * @param columnIndexResolver column-index lookup, memoized once per {@code getRow}-style call so
   *                            it is not re-resolved on every row/column access during polling
   * @param condition           condition how to filter all rows in the table
   * @return the first matching row, or {@code null} if none matched (yet)
   */
  private Row<T> findFirstMatch(Function<T, Integer> columnIndexResolver, Predicate<Row<T>> condition) {
    try {
      return getAllRows(columnIndexResolver).stream().filter(condition).findFirst()
          .map(row -> mapToRow(row.getSelf(), fetchColumnIndex())).orElse(null);
    } catch (UIAssertionError | NoSuchElementException | StaleElementReferenceException e) {
      return null;
    }
  }

  /**
   * Wrap {@link #fetchColumnIndex()} so a given column's index is only resolved once (per call)
   * instead of on every row/column access during a poll loop.
   *
   * @return memoizing column-index lookup
   */
  private Function<T, Integer> memoizeColumnIndex() {
    Map<T, Integer> cache = new ConcurrentHashMap<>();
    Function<T, Integer> resolver = fetchColumnIndex();
    return columnHeader -> cache.computeIfAbsent(columnHeader, resolver);
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
    return getAllRows(fetchColumnIndex());
  }

  /**
   * Get all table rows, resolving each row's column index through the given resolver instead of
   * a fresh call to {@link #fetchColumnIndex()} per row, so a resolver memoized for the duration
   * of a poll loop (see {@link #memoizeColumnIndex()}) is reused across every poll iteration.
   *
   * @param columnIndexResolver column-index lookup to hand to every mapped row
   * @return all table {@link Row} element
   */
  private List<Row<T>> getAllRows(Function<T, Integer> columnIndexResolver) {
    return getAllRowsElements().asFixedIterable().stream()
        .map(element -> mapToRow(element, columnIndexResolver)).collect(Collectors.toList());
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
    ElementsCollection rowsWithHeader = this.getSelf().shouldBe(Condition.visible).findAll(By.tagName(HtmlTag.TR));
    return rowsWithHeader.last(Math.max(0, rowsWithHeader.size() - HTML_START_INDEX));
  }
}
