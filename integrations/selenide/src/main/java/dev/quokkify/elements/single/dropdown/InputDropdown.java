package dev.quokkify.elements.single.dropdown;

import java.util.Objects;

import dev.quokkify.elements.base.Component;
import dev.quokkify.html.model.HtmlTag;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import org.openqa.selenium.By;

/**
 * Component object for an input-backed custom combobox.
 *
 * <p>The public operations describe combobox orchestration: type a query, select a matching
 * option, and leave the widget closed. Native element operations remain available through
 * Selenide and are intentionally not duplicated here.
 */
public class InputDropdown extends Component {

  protected By getInputSelector() {
    return By.cssSelector(HtmlTag.INPUT);
  }

  protected By getSelectedOptionsSelector() {
    return By.cssSelector(".item");
  }

  protected By getSelectedOptionLabelSelector() {
    return By.cssSelector("[data-dropdown-label], .item-label, .label");
  }

  protected By getSelectedOptionExcludedSelector() {
    return null;
  }

  protected By getOptionsContainerSelector() {
    return By.cssSelector("[role='listbox'], .dropdown-menu, .menu");
  }

  protected By getOptionsSelector() {
    return By.cssSelector("[role='option'], li, .option");
  }

  /** Types a query and selects the exact matching custom option. */
  public void typeAndSelectExact(String text) {
    type(text);
    selectOptionByCondition(Condition.exactText(text));
    closeDropdown();
  }

  /** Types a query and selects the first custom option containing the query. */
  public void typeAndSelectPartial(String text) {
    type(text);
    selectOptionByCondition(Condition.partialText(text));
    closeDropdown();
  }

  /** Selects an exact option, opening the combobox only when necessary. */
  public void selectExact(String text) {
    openDropdown();
    selectOptionByCondition(Condition.exactText(text));
    closeDropdown();
  }

  /** Selects the first option containing the supplied text. */
  public void selectPartial(String text) {
    openDropdown();
    selectOptionByCondition(Condition.partialText(text));
    closeDropdown();
  }

  /** Types text and accepts the widget's currently highlighted option. */
  public void typeAndPressEnter(String text) {
    type(text);
    getInput().pressEnter();
    closeDropdown();
  }

  protected final SelenideElement getInput() {
    return getSelf().find(getInputSelector());
  }

  protected final SelenideElement getOptionsContainer() {
    return getSelf().find(getOptionsContainerSelector());
  }

  protected final void type(String text) {
    getInput().setValue(text);
  }

  protected final void clearInputValue() {
    getInput().clear();
  }

  protected final String getSelectedOptionLabelText(SelenideElement selectedOption) {
    String selectedText = selectedOption.getText();
    String labelText = selectedOption.findAll(getSelectedOptionLabelSelector()).texts().stream()
        .map(String::trim)
        .filter(text -> !text.isEmpty())
        .findFirst()
        .orElseGet(() -> extractFallbackSelectedOptionText(selectedOption, selectedText));
    return Objects.requireNonNullElse(labelText, selectedText).trim();
  }

  private String extractFallbackSelectedOptionText(SelenideElement selectedOption, String selectedText) {
    By excludedTextSelector = getSelectedOptionExcludedSelector();
    if (excludedTextSelector == null) {
      return selectedText;
    }
    String fallbackText = Selenide.executeJavaScript(
        "const clone = arguments[0].cloneNode(true);"
            + "clone.querySelectorAll(arguments[1]).forEach(element => element.remove());"
            + "return (clone.textContent || '').trim();",
        selectedOption,
        excludedTextSelector.toString().replaceFirst("^By\\.cssSelector: ", ""));
    return fallbackText == null || fallbackText.isBlank() ? selectedText : fallbackText;
  }

  protected final void selectOptionByCondition(WebElementCondition condition) {
    getOptionsContainer().findAll(getOptionsSelector()).filter(condition).first().click();
  }

  private void openDropdown() {
    if (!isDropdownOpen()) {
      getInput().click();
    }
  }

  protected final void closeDropdown() {
    if (isDropdownOpen()) {
      getInput().pressEscape();
    }
  }

  private boolean isDropdownOpen() {
    return getOptionsContainer().is(Condition.visible);
  }
}
