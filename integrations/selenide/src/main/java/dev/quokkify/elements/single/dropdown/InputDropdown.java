package dev.quokkify.elements.single.dropdown;

import java.util.List;

import dev.quokkify.html.model.HtmlTag;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

/**
 * Abstract dropdown which has an input with possibility to print the text and select any matches.
 */
public class InputDropdown extends AbstractDropdown {

  /**
   * Dropdown selector for input element.
   */
  protected By getInputSelector() {
    return By.cssSelector(HtmlTag.INPUT);
  }

  /**
   * Dropdown selector for selected options elements.
   */
  protected By getSelectedOptionsSelector() {
    return By.cssSelector(".item");
  }

  /**
   * Dropdown selector for options items container.
   */
  @Override
  protected By getOptionsContainerSelector() {
    return By.cssSelector("[role='listbox'], .dropdown-menu, .menu");
  }

  /**
   * Set value to dropdown input and press enter.
   */
  public void sendKeysAndPressEnter(String value) {
    SelenideElement inputElement = getSelf().find(getInputSelector());
    inputElement.sendKeys(value);
    inputElement.pressEnter();
    closeDropdown();
  }

  /**
   * Find option by the expected option partial text.
   *
   * @param text expected option partial text
   */
  public void sendKeysAndChoseOptionByPartialText(String text) {
    sendKeys(text);
    findAndSelectOptionByPartialText(text);
  }

  /**
   * Find option by the expected option exact text.
   *
   * @param text expected option exact text
   */
  public void sendKeysAndChoseOptionByExactText(String text) {
    sendKeys(text);
    findAndSelectOptionByExactText(text);
  }

  /**
   * Send keys.
   *
   * @param text text to send.
   */
  public void sendKeys(String text) {
    this.getSelf().find(getInputSelector()).sendKeys(text);
  }

  @Override
  public void selectOption(String value) {
    openAndChoseOptionByPartialText(value);
    closeDropdown();
  }

  @Override
  public void selectOptionByExactText(String text) {
    openAndChoseOptionByExactText(text);
    closeDropdown();
  }

  @Override
  public void selectOptionByPartialText(String text) {
    openAndChoseOptionByPartialText(text);
    closeDropdown();
  }

  /**
   * Open dropdown menu and chose the option by partial text without closing.
   *
   * @param value partial visible text of option
   */
  public void openAndChoseOptionByPartialText(String value) {
    openDropdown();
    findAndSelectOptionByPartialText(value);
  }

  /**
   * Open dropdown menu and chose the option by exact text without closing.
   *
   * @param value exact visible text of option
   */
  public void openAndChoseOptionByExactText(String value) {
    openDropdown();
    findAndSelectOptionByExactText(value);
  }

  /**
   * Get input value.
   *
   * @return input value as {@link String}
   */
  public String getInputValue() {
    return this.getSelf().find(getInputSelector()).getValue();
  }

  @Override
  public String getSelectedOptionText() {
    return this.getSelf().findAll(getSelectedOptionsSelector()).texts().toString().replaceAll("[\\[\\]]", StringUtils.EMPTY);
  }

  @Override
  public List<String> getOptionsTexts() {
    openDropdown();
    List<String> optionsTexts = getOptions().texts();
    closeDropdown();
    return optionsTexts;
  }

  @Override
  public int getSize() {
    return getOptions().size();
  }

  /**
   * Make the dropdown input clear.
   */
  public void clear() {
    openDropdown();
    this.getSelf().find(getInputSelector()).sendKeys(Keys.BACK_SPACE);
    closeDropdown();
  }

  /**
   * Find selected option element by partial text.
   *
   * @param text expected option partial text
   */
  public SelenideElement findSelectedOptionByPartialText(String text) {
    return this.getSelf().findAll(getSelectedOptionsSelector()).findBy(Condition.partialText(text));
  }

  /**
   * Find a dropdown option by the expected option partial text.
   *
   * @param value expected option partial text
   */
  public void findAndSelectOptionByPartialText(String value) {
    findAndSelectOptionByCondition(Condition.partialText(value));
  }

  /**
   * Find a dropdown option by the expected option exact text.
   *
   * @param value expected option exact text
   */
  public void findAndSelectOptionByExactText(String value) {
    findAndSelectOptionByCondition(Condition.exactText(value));
  }

  /**
   * Find a dropdown option by filtering condition.
   *
   * @param condition filtered condition
   */
  protected void findAndSelectOptionByCondition(WebElementCondition condition) {
    getOptions().findBy(condition).click();
  }

  /**
   * Check is dropdown closed and open the dropdown if needed.
   */
  public void openDropdown() {
    if (isClosed()) {
      toggle();
    }
  }

  /**
   * Check is dropdown opened and close the dropdown if needed.
   */
  public void closeDropdown() {
    if (isOpened()) {
      this.getSelf().find(getInputSelector()).pressEscape();
    }
  }

  /**
   * Get dropdown enabled status.
   *
   * @return dropdown enabled status as {@link Boolean}
   */
  public boolean isEnabled() {
    return !isDisabled();
  }

  /**
   * Get dropdown disabled status.
   *
   * @return dropdown disabled status as {@link Boolean}
   */
  public boolean isDisabled() {
    return getSelf().find(getInputSelector()).is(Condition.disabled);
  }
}
