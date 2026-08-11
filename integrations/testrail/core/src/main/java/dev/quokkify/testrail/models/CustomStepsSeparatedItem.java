package dev.quokkify.testrail.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomStepsSeparatedItem {

  private String expected;
  private String content;

  public CustomStepsSeparatedItem() {
  }

  public CustomStepsSeparatedItem(String expected, String content) {
    this.expected = expected;
    this.content = content;
  }

  public String getExpected() {
    return expected;
  }

  public CustomStepsSeparatedItem setExpected(String expected) {
    this.expected = expected;
    return this;
  }

  public String getContent() {
    return content;
  }

  public CustomStepsSeparatedItem setContent(String content) {
    this.content = content;
    return this;
  }
}
