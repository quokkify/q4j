package dev.quokkify.elements.table.horizontal;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.quokkify.elements.base.BaseTable;
import dev.quokkify.ex.TableRowException;
import dev.quokkify.html.model.HtmlTag;
import dev.quokkify.model.ConstantFormat;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import com.codeborne.selenide.ex.UIAssertionError;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.Configuration.timeout;

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
    return getRow(columnHeader, Duration.ofMillis(timeout));
  }

  /**
   * Get row by column, waiting for it to appear.
   *
   * @param columnHeader    column enum
   * @param timeout         how long to wait for the row
   * @return horizontal table {@link HorizontalRow} element
   */
  public HorizontalRow<T> getRow(T columnHeader, Duration timeout) {
    MatchingHorizontalRowCondition matchingRow = new MatchingHorizontalRowCondition(columnHeader);
    SelenideElement matchingElement = getSelf().findAll(By.tagName(HtmlTag.TR))
        .findBy(matchingRow)
        .find(By.tagName(HtmlTag.TD));
    try {
      matchingElement.shouldBe(Condition.exist, timeout);
      return new HorizontalRow<>(matchingElement);
    } catch (UIAssertionError error) {
      throw new TableRowException(columnHeader, timeout, error);
    }
  }

  private final class MatchingHorizontalRowCondition extends WebElementCondition {

    private final T columnHeader;

    private MatchingHorizontalRowCondition(T columnHeader) {
      super("row for '%s' header".formatted(columnHeader));
      this.columnHeader = columnHeader;
    }

    @Override
    public CheckResult check(Driver driver, WebElement row) {
      try {
        WebElement table = row.findElement(By.xpath("./ancestor::%s[1]".formatted(HtmlTag.TABLE)));
        List<WebElement> rows = table.findElements(By.tagName(HtmlTag.TR));
        int rowIndex = fetchColumnIndex(driver, table, columnHeader);
        int candidateIndex = rows.indexOf(row);
        List<WebElement> cells = row.findElements(By.tagName(HtmlTag.TD));
        if (candidateIndex == rowIndex && !cells.isEmpty()) {
          return CheckResult.accepted("matched row: " + cells.get(0).getText());
        }
        return CheckResult.rejected("Row does not match yet", "candidate index: " + candidateIndex);
      } catch (NoSuchElementException | StaleElementReferenceException error) {
        return CheckResult.rejected(error.toString(), "table changed while checking rows");
      }
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
        .collect(Collectors.toMap(keys::get, values::get, (left, right) -> right, LinkedHashMap::new));
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
