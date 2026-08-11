package dev.quokkify.elements.table.classic.base;

import javax.annotation.Nonnull;

import dev.quokkify.elements.base.Component;

import com.codeborne.selenide.SelenideElement;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.By;

/**
 * Table column UI element and methods of working with it.
 *
 * @param <T> enum with columns enumerations
 */
public class BaseColumn<T extends Enum<T>> extends Component {

  private SelenideElement element;

  public BaseColumn(SelenideElement selenideElement) {
    this.element = selenideElement;
  }

  /**
   * Verify column name in table.
   *
   * @param expectedColumnName expected column name
   */
  public void verifyColumn(String expectedColumnName) {
    Assertions.assertThat(this.getSelf().getText())
        .as("Column name incorrect")
        .isEqualToIgnoringCase(expectedColumnName);
  }

  /**
   * Check that columns sortable.
   *
   * @return true and false
   */
  public boolean isColumnSortable() {
    return this.getSelf().find(By.xpath("./descendant-or-self::*[contains(@class, 'sortable')]"))
        .isDisplayed();
  }

  @Nonnull
  @Override
  public SelenideElement getSelf() {
    return element;
  }
}
