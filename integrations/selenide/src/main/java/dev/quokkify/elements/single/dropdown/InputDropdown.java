package dev.quokkify.elements.single.dropdown;

import dev.quokkify.elements.base.Component;
import dev.quokkify.html.model.HtmlTag;

import com.codeborne.selenide.Condition;
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

  /** Clears the input-backed combobox query without toggling its popup. */
  public void clearInput() {
    getInput().clear();
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

  protected final void selectOptionByCondition(WebElementCondition condition) {
    getOptionsContainer().findAll(getOptionsSelector()).findBy(condition).click();
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
