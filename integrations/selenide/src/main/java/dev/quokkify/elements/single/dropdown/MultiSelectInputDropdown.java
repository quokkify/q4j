package dev.quokkify.elements.single.dropdown;

import java.util.List;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

/** Component object for a custom combobox that renders removable selected chips. */
public class MultiSelectInputDropdown extends InputDropdown {

  protected By removeOptionButtonSelector() {
    return By.cssSelector("[class^='remove']");
  }

  /** Selects each option by exact visible text and closes the popup. */
  public void selectAllExact(List<String> texts) {
    texts.forEach(this::selectExact);
  }

  /** Selects each option by partial visible text and closes the popup after each selection. */
  public void selectAllPartial(List<String> texts) {
    texts.forEach(this::selectPartial);
  }

  /** Removes every currently selected chip using the widget's remove controls. */
  public void clearSelected() {
    SelenideElement input = getInput();
    while (getSelf().findAll(getSelectedOptionsSelector()).size() > 0) {
      input.sendKeys(Keys.BACK_SPACE);
    }
    closeDropdown();
  }

  /** Returns selected chip labels as individual values, without parsing rendered list text. */
  public List<String> selectedTexts() {
    return getSelf().findAll(getSelectedOptionsSelector()).texts();
  }

  /** Removes the first selected chip containing the supplied label. */
  public void removeSelectedPartial(String text) {
    getSelf().findAll(getSelectedOptionsSelector())
        .findBy(com.codeborne.selenide.Condition.partialText(text))
        .find(removeOptionButtonSelector())
        .click();
    closeDropdown();
  }
}
