package dev.quokkify.testrail.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Options {

  @JsonProperty("is_required")
  private boolean isRequired;
  private String format;
  @JsonProperty("default_value")
  private String defaultValue;
  private String rows;
  @JsonProperty("has_expected")
  private boolean hasExpected;
  private String items;

  public Options() {
  }

  public boolean isRequired() {
    return isRequired;
  }

  public Options setRequired(boolean required) {
    isRequired = required;
    return this;
  }

  public String getFormat() {
    return format;
  }

  public Options setFormat(String format) {
    this.format = format;
    return this;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public Options setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  public String getRows() {
    return rows;
  }

  public Options setRows(String rows) {
    this.rows = rows;
    return this;
  }

  public boolean isHasExpected() {
    return hasExpected;
  }

  public Options setHasExpected(boolean hasExpected) {
    this.hasExpected = hasExpected;
    return this;
  }

  public String getItems() {
    return items;
  }

  public Options setItems(String items) {
    this.items = items;
    return this;
  }
}
