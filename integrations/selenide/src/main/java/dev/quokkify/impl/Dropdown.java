package dev.quokkify.impl;

import java.util.List;

/**
 * Interface for Dropdown elements.
 */
public interface Dropdown {

  /**
   * Select an option from dropdown list (by text).
   *
   * @param value visible text of option
   */
  void selectOption(String value);

  /**
   * Get text of selected option in select field.
   *
   * @return text of selected option
   */
  String getSelectedOptionText();

  /**
   * Check if dropdown contains value within options.
   *
   * @param value to check
   * @return result of matching the value within options
   */
  default boolean containsOptionValue(String value) {
    return this.getOptionsTexts().stream().anyMatch(option -> option.equals(value));
  }

  /**
   * Gets all the texts of dropdown options.
   *
   * @return texts of dropdown options
   */
  List<String> getOptionsTexts();

  /**
   * Gets size of dropdown options.
   *
   * @return size of dropdown options
   */
  int getSize();
}
