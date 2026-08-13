package dev.quokkify.elements.single.dropdown;

import java.util.List;

import dev.quokkify.elements.base.Component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

/**
 * Base dropdown component for custom widget implementations.
 */
public abstract class AbstractDropdown extends Component {

  /**
   * Dropdown selector for options items.
   */
  protected By getOptionsSelector() {
    return By.xpath(".//*[@role='option'] | .//li | .//*[contains(@class,'option')]");
  }

  /**
   * Dropdown selector for options container.
   */
  protected By getOptionsContainerSelector() {
    return By.cssSelector("[role='listbox']");
  }

  /**
   * Get options container element.
   *
   * @return options container element
   */
  protected SelenideElement getOptionsContainer() {
    return this.getSelf().find(getOptionsContainerSelector());
  }

  /**
   * Select an option from dropdown list (by text).
   *
   * @param value visible text of option
   */
  public void selectOption(String value) {
    selectOptionByPartialText(value);
  }

  /**
   * Select dropdown option by exact text (with opening and closing dropdown).
   *
   * @param text exact visible text of option
   */
  public void selectOptionByExactText(String text) {
    toggle();
    selectOptionByExactTextWithoutToggle(text);
    toggle();
  }

  /**
   * Select dropdown option by exact text (without opening and closing dropdown).
   *
   * @param text exact visible text of option
   */
  public void selectOptionByExactTextWithoutToggle(String text) {
    getOptions().findBy(Condition.exactText(text)).click();
  }

  /**
   * Select dropdown option by partial text (with opening and closing dropdown).
   *
   * @param text partial visible text of option
   */
  public void selectOptionByPartialText(String text) {
    toggle();
    selectOptionByPartialTextWithoutToggle(text);
    toggle();
  }

  /**
   * Select dropdown option by partial text (without opening and closing dropdown).
   *
   * @param text partial visible text of option
   */
  public void selectOptionByPartialTextWithoutToggle(String text) {
    getOptions().findBy(Condition.partialText(text)).click();
  }

  /**
   * Get dropdown opened status.
   *
   * @return dropdown opened status as {@link Boolean}
   */
  public boolean isOpened() {
    return getOptionsContainer().is(Condition.visible);
  }

  /**
   * Get dropdown closed status.
   *
   * @return dropdown closed status as {@link Boolean}
   */
  public boolean isClosed() {
    return getOptionsContainer().is(Condition.hidden);
  }

  /**
   * Open dropdown menu if it is currently closed.
   */
  public void openDropdown() {
    if (isClosed()) {
      toggle();
    }
  }

  /**
   * Close dropdown menu if it is currently opened.
   */
  public void closeDropdown() {
    if (isOpened()) {
      toggle();
    }
  }

  /**
   * Toggle a dropdown.
   */
  public void toggle() {
    this.getSelf().click();
  }

  /**
   * Get dropdown options items elements.
   *
   * @return options items elements as {@link ElementsCollection}
   */
  protected ElementsCollection getOptions() {
    return getOptionsContainer().findAll(getOptionsSelector());
  }

  /**
   * Gets the selected option text.
   *
   * @return selected option text
   */
  public String getSelectedOptionText() {
    return this.getSelf().getText();
  }

  /**
   * Gets all the texts of dropdown options.
   *
   * @return texts of dropdown options
   */
  public List<String> getOptionsTexts() {
    openDropdown();
    List<String> texts = getOptions().texts();
    closeDropdown();
    return texts;
  }

  /**
   * Gets size of dropdown options.
   *
   * @return size of dropdown options
   */
  public int getSize() {
    openDropdown();
    int size = getOptions().size();
    closeDropdown();
    return size;
  }
}
