package dev.quokkify.elements.single.dropdown;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.codeborne.selenide.SelenideElement;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

/**
 * Abstract dropdown which has an input with possibility to print the text and select any matches.
 * It's possible to select multiply options
 */
public class MultiSelectInputDropdown extends InputDropdown {

  /**
   * Dropdown selector for options remove button.
   */
  protected By removeOptionButtonSelector() {
    return By.cssSelector("[class^='remove']");
  }

  /**
   * Find options by the expected option texts.
   *
   * @param values expected option texts
   */
  public void sendKeys(List<String> values) {
    values.forEach(this::sendKeysAndChoseOptionByPartialText);
    closeDropdown();
  }

  /**
   * Select an options from dropdown list (by exact text).
   *
   * @param texts text values of options
   */
  public void selectOptionsByExactText(List<String> texts) {
    this.openDropdown();
    texts.forEach(this::findAndSelectOptionByExactText);
    this.closeDropdown();
  }

  public void selectOptions(List<String> texts) {
    openDropdown();
    texts.forEach(this::findAndSelectOptionByPartialText);
    closeDropdown();
  }

  public void clear() {
    SelenideElement input = getSelf().find(getInputSelector());
    int selectedOptionCount = getSelf().findAll(getSelectedOptionsSelector()).size();
    IntStream.range(0, selectedOptionCount).forEach(index -> input.sendKeys(Keys.BACK_SPACE));
    closeDropdown();
  }

  public List<String> getSelectedOptionsTexts() {
    return Arrays.stream(getSelectedOptionText().split("\n×, "))
        .map(option -> option.replaceAll("\n×", StringUtils.EMPTY))
        .collect(Collectors.toList());
  }

  /**
   * Remove option by partial text using option 'remove' button.
   *
   * @param text expected option partial text
   */
  public void removeOptionByPartialText(String text) {
    findSelectedOptionByPartialText(text).find(removeOptionButtonSelector()).click();
    closeDropdown();
  }
}
