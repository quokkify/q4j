package dev.quokkify.elements.table.classic;

import java.util.List;
import java.util.function.Function;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Flex Table UI element and methods of working with it.
 * Table consist of react flex table rows.
 *
 * @param <T> enum with columns enumerations
 */
public class FlexTable<T extends Enum<T>> extends Table<T> {

  @Override
  public List<String> getAllColumnValuesByXpath(T columnHeader, String xpathAdditionalLocator) {
    return this.getSelf().findAll(By.xpath(".//div[%d]%s".formatted(this
        .fetchColumnIndex().apply(columnHeader) + HTML_START_INDEX, xpathAdditionalLocator))).texts();
  }

  @Override
  protected ElementsCollection getAllRowsElements() {
    ElementsCollection rowsWithHeader = this.getSelf()
        .findAll(By.cssSelector(".flex-table-row"));
    return rowsWithHeader.last(Math.max(0, rowsWithHeader.size() - HTML_START_INDEX));
  }

  @Override
  protected List<WebElement> getAllRowsElements(WebElement table) {
    List<WebElement> rowsWithHeader = table.findElements(By.cssSelector(".flex-table-row"));
    return rowsWithHeader.size() <= HTML_START_INDEX
        ? List.of()
        : rowsWithHeader.subList(HTML_START_INDEX, rowsWithHeader.size());
  }

  /**
   * Create row by selenide element, reusing the given (possibly memoized) column-index resolver
   * instead of calling {@link #fetchColumnIndex()} again on every cell access.
   *
   * @param element             selenide element for table row
   * @param columnIndexResolver column-index lookup for cells of the mapped row
   * @return table {@link Row} element
   */
  @Override
  protected Row<T> mapToRow(SelenideElement element, Function<T, Integer> columnIndexResolver) {
    return new Row<>(element, columnIndexResolver) {

      @Override
      public Cell<T> getCell(T columnHeader) {
        int cellIndex = columnIndexResolver.apply(columnHeader) + HTML_START_INDEX;
        return new Cell<>(this.getSelf().find(By.xpath("./div[%d]".formatted(cellIndex))), this);
      }
    };
  }
}
