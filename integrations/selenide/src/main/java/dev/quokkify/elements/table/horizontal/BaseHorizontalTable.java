package dev.quokkify.elements.table.horizontal;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.quokkify.elements.base.BaseTable;
import dev.quokkify.ex.TableRowException;
import dev.quokkify.html.model.HtmlTag;
import dev.quokkify.model.ConstantFormat;
import dev.quokkify.util.Waiter;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.UIAssertionError;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;

/**
 * Abstract class to work with Horizontal Table.
 *
 * @param <T> enum with columns enumerations
 */
public abstract class BaseHorizontalTable<T extends Enum<T> & ConstantFormat> extends BaseTable<T> {

  /**
   * Get row by column, waiting for it to appear with the default timeout.
   *
   * @param columnHeader column enum
   * @return horizontal table {@link HorizontalRow} element
   */
  public HorizontalRow<T> getRow(T columnHeader) {
    return getRow(columnHeader, DEFAULT_ROW_TIMEOUT, DEFAULT_ROW_POLLING_INTERVAL);
  }

  /**
   * Get row by column, waiting for it to appear.
   *
   * @param columnHeader    column enum
   * @param timeout         how long to wait for the row
   * @param pollingInterval how often to re-read the table rows
   * @return horizontal table {@link HorizontalRow} element
   */
  public HorizontalRow<T> getRow(T columnHeader, Duration timeout, Duration pollingInterval) {
    try {
      return Waiter.awaitCondition(() -> resolveRow(columnHeader), Matchers.notNullValue(),
          "Waiting for horizontal table row", timeout, pollingInterval);
    } catch (ConditionTimeoutException e) {
      throw new TableRowException(columnHeader, timeout, e);
    }
  }

  /**
   * Resolve the row for the given column, tolerating the exceptions that
   * {@link #fetchColumnIndex()} or {@link #getAllRows()} can throw while the table is still
   * mounting asynchronously, so the caller's poll loop keeps retrying instead of aborting on the
   * first failed read.
   *
   * <p>The column index is resolved fresh on every poll rather than cached before the wait
   * starts, so a text-driven lookup (e.g. {@link DynamicHorizontalTable}) keeps re-checking the
   * live DOM instead of being stuck with a stale index from before rows finished rendering. The
   * index and the row list are still two independent reads, so a row insertion landing between
   * them can still shift the index by one poll; that window is narrowed to a single poll rather
   * than eliminated.
   *
   * @param columnHeader column enum
   * @return the matching row, or {@code null} if it is not resolvable/present yet
   */
  private HorizontalRow<T> resolveRow(T columnHeader) {
    try {
      int rowIndex = fetchColumnIndex().apply(columnHeader);
      List<HorizontalRow<T>> rows = getAllRows();
      return rowIndex >= 0 && rows.size() > rowIndex ? rows.get(rowIndex) : null;
    } catch (UIAssertionError | NoSuchElementException | StaleElementReferenceException e) {
      return null;
    }
  }

  /**
   * Get existing row status by row header.
   *
   * @param columnHeader column enum
   * @return true if any row has provided header, otherwise false
   */
  public boolean isRowExist(T columnHeader) {
    return isRowExist(columnHeader.upperCaseWithSpace());
  }

  /**
   * Get existing row status by row header.
   *
   * @param columnHeaderTitle column header title
   * @return true if any row has provided header, otherwise false
   */
  public boolean isRowExist(String columnHeaderTitle) {
    return getAllColumns().asDynamicIterable().stream().anyMatch(row -> row.text().equals(columnHeaderTitle));
  }

  @Override
  public HorizontalRow<T> getFirstRow() {
    return getAllRows().stream()
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No horizontal rows found"));
  }

  /**
   * Get columns and values as map.
   *
   * @return {@link Map}
   */
  public Map<String, String> columnsAndValuesAsMap() {
    List<String> keys = getAllColumnsNames();
    List<String> values = getAllRowsValues();
    return IntStream.range(0, keys.size()).boxed()
        .collect(Collectors.toMap(keys::get, values::get));
  }

  /**
   * Get all rows as values.
   *
   * @return list of rows values
   */
  private List<String> getAllRowsValues() {
    return getAllRows().stream()
        .map(row -> row.getSelf().getText())
        .collect(Collectors.toList());
  }

  @Override
  public List<HorizontalRow<T>> getAllRows() {
    return this.getSelf().findAll(By.tagName(HtmlTag.TD)).asFixedIterable().stream()
        .map((Function<SelenideElement, HorizontalRow<T>>) HorizontalRow::new).collect(Collectors.toList());
  }
}
