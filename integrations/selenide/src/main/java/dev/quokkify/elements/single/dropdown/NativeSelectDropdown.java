package dev.quokkify.elements.single.dropdown;

import java.util.List;

import dev.quokkify.elements.base.Component;

import com.codeborne.selenide.ElementsCollection;
import org.openqa.selenium.By;

/**
 * Native <select> dropdown backed by Selenide's selectOption API.
 */
public class NativeSelectDropdown extends Component {

  /**
   * Select an option from dropdown list (by text).
   *
   * @param value visible text of option
   */
  public void selectOption(String value) {
    this.getSelf().selectOption(value);
  }

  /**
   * Select an option from dropdown list (by value attribute).
   *
   * @param value option value attribute
   */
  public void selectOptionByValue(String value) {
    this.getSelf().selectOptionByValue(value);
  }

  /**
   * Get text of selected option in select field.
   *
   * @return text of selected option
   */
  public String getSelectedOptionText() {
    return this.getSelf().getSelectedOptionText();
  }

  /**
   * Gets all the texts of dropdown options.
   *
   * @return texts of dropdown options
   */
  public List<String> getOptionsTexts() {
    return getOptions().texts();
  }

  /**
   * Gets size of dropdown options.
   *
   * @return size of dropdown options
   */
  public int getSize() {
    return getOptions().size();
  }

  /**
   * Get native option elements.
   *
   * @return option elements collection
   */
  protected ElementsCollection getOptions() {
    return this.getSelf().findAll(By.tagName("option"));
  }
}
