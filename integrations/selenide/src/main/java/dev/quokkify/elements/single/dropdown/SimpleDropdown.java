package dev.quokkify.elements.single.dropdown;

import java.util.List;

import dev.quokkify.elements.base.Component;
import dev.quokkify.impl.Dropdown;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import org.openqa.selenium.By;

/**
 * Dropdown menu which has just one functionality to select single value.
 * Locator should point right on the 'select'
 */
public class SimpleDropdown extends Component implements Dropdown {

  /**
   * Dropdown selector for options items.
   */
  protected By getOptionsSelector() {
    return By.xpath(".//option | .//*[@role='option'] | .//*[contains(@class,'option')] | .//li");
  }

  /**
   * Dropdown selector for element that has dropdown expand status.
   */
  protected By getExpandStatusSelector() {
    return getOptionsContainerSelector();
  }

  /**
   * Dropdown selector for options items container.
   */
  protected By getOptionsContainerSelector() {
    return By.xpath(".");
  }

  @Override
  public void selectOption(String text) {
    selectOptionByPartialText(text);
  }

  /**
   * Select dropdown option by value.
   *
   * @param value value of option
   */
  public void selectOptionByValue(String value) {
    this.getSelf().selectOptionByValue(value);
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
    getOptions().filter(Condition.exactText(text)).first().click();
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
    getOptions().filter(Condition.partialText(text)).first().click();
  }

  @Override
  public String getSelectedOptionText() {
    return this.getSelf().getSelectedOptionText();
  }

  @Override
  public int getSize() {
    toggle();
    int size = getOptions().size();
    toggle();
    return size;
  }

  @Override
  public List<String> getOptionsTexts() {
    toggle();
    List<String> texts = getOptions().texts();
    toggle();
    return texts;
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
    return getSelf().find(getOptionsContainerSelector()).findAll(getOptionsSelector());
  }
}
