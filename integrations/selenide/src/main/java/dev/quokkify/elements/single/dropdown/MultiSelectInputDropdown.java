package dev.quokkify.elements.single.dropdown;

import java.time.Duration;
import java.util.List;

import dev.quokkify.util.Waiter;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.awaitility.core.ConditionTimeoutException;
import org.openqa.selenium.By;

/** Component object for a custom combobox that renders removable selected chips. */
public class MultiSelectInputDropdown extends InputDropdown {

  private static final Duration CHIP_REMOVAL_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration CHIP_REMOVAL_POLLING_INTERVAL = Duration.ofMillis(100);

  protected String removeOptionButtonCssSelector() {
    return "[class^='remove']";
  }

  @Override
  protected String getFallbackSelectedOptionLabelText(SelenideElement selectedOption, String selectedText) {
    String fallbackText = Selenide.executeJavaScript(
        "const clone = arguments[0].cloneNode(true);"
            + "clone.querySelectorAll(arguments[1]).forEach(element => element.remove());"
            + "return (clone.textContent || '').trim();",
        selectedOption,
        removeOptionButtonCssSelector());
    return fallbackText == null || fallbackText.isBlank() ? selectedText : fallbackText;
  }

  protected By removeOptionButtonSelector() {
    return By.cssSelector(removeOptionButtonCssSelector());
  }

  protected Duration chipRemovalTimeout() {
    return CHIP_REMOVAL_TIMEOUT;
  }

  protected Duration chipRemovalPollingInterval() {
    return CHIP_REMOVAL_POLLING_INTERVAL;
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
    clearInputValue();
    while (getSelf().findAll(getSelectedOptionsSelector()).size() > 0) {
      removeSelectedAt(0);
    }
    closeDropdown();
  }

  /** Returns selected chip labels as individual values, without parsing rendered list text. */
  public List<String> selectedTexts() {
    return getSelf().findAll(getSelectedOptionsSelector()).stream()
        .map(this::getSelectedOptionLabelText)
        .toList();
  }

  /** Removes the first selected chip containing the supplied label. */
  public void removeSelectedPartial(String text) {
    SelenideElement chip = getSelf().findAll(getSelectedOptionsSelector()).stream()
        .filter(selectedOption -> getSelectedOptionLabelText(selectedOption).contains(text))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Selected chip containing '%s' was not found".formatted(text)));
    clickAndAwaitRemoval(chip, getSelectedOptionLabelText(chip));
    closeDropdown();
  }

  private void removeSelectedAt(int index) {
    SelenideElement chip = getSelf().findAll(getSelectedOptionsSelector()).get(index);
    clickAndAwaitRemoval(chip, getSelectedOptionLabelText(chip));
  }

  private void clickAndAwaitRemoval(SelenideElement chip, String chipLabel) {
    int beforeCount = getSelf().findAll(getSelectedOptionsSelector()).size();
    chip.find(removeOptionButtonSelector()).click();
    try {
      Waiter.awaitCondition(
          () -> getSelf().findAll(getSelectedOptionsSelector()).size() < beforeCount,
          "Chip '%s' was not removed".formatted(chipLabel),
          chipRemovalTimeout(),
          chipRemovalPollingInterval());
    } catch (ConditionTimeoutException timeoutException) {
      throw new IllegalStateException(
          "No progress while removing selected chip '%s' from dropdown".formatted(chipLabel),
          timeoutException);
    }
  }
}
