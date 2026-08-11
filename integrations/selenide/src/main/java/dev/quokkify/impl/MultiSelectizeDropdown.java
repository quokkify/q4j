package dev.quokkify.impl;

import java.util.List;

public interface MultiSelectizeDropdown extends Dropdown {

  /**
   * Select an options from dropdown list (by text).
   *
   * @param values visible text(s) of option
   */
  void selectOptions(List<String> values);

  /**
   * Get texts of selected options in select field.
   *
   * @return texts of selected options
   */
  List<String> getSelectedOptionsTexts();
}
