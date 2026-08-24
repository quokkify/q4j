package dev.quokkify.elements.table.classic;

import java.util.List;
import java.util.function.Function;

import dev.quokkify.elements.table.model.DomTableLayout;

import com.codeborne.selenide.Driver;
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
  protected DomTableLayout domTableLayout() {
    return DomTableLayout.FLEX;
  }

  @Override
  public List<String> getAllColumnValuesByXpath(T columnHeader, String xpathAdditionalLocator) {
    return this.getSelf().findAll(By.xpath(
        "(.//*[contains(concat(' ', normalize-space(@class), ' '), ' flex-table-row ')])[position() > 1]/div[%d]%s"
            .formatted(this.fetchColumnIndex().apply(columnHeader) + HTML_START_INDEX, xpathAdditionalLocator)))
        .texts();
  }

  @Override
  protected ElementsCollection getAllRowsElements() {
    ElementsCollection rowsWithHeader = this.getSelf()
        .findAll(By.cssSelector(".flex-table-row"));
    return rowsWithHeader.last(Math.max(0, rowsWithHeader.size() - HTML_START_INDEX));
  }

  @Override
  protected ElementsCollection getRowsForLookup() {
    return getSelf().findAll(By.xpath(
        "(.//*[contains(concat(' ', normalize-space(@class), ' '), ' flex-table-row ')])[position() > 1]"));
  }

  @Override
  protected int fetchColumnIndexForRow(Driver driver, WebElement rowElement, T columnHeader) {
    return fetchColumnIndex().apply(columnHeader);
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
