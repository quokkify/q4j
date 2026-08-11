package dev.quokkify.testrail.models;

import dev.quokkify.model.ConstantFormat;

public enum CustomAutomationTypes implements ConstantFormat {
  TO_AUTOMATE, AUTOMATED, MANUAL;

  @Override
  public String formatValue() {
    return name();
  }
}
