package dev.quokkify.testrail.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StepResult {

  private String content;
  private String expected;
  private String actual;
  @JsonProperty("status_id")
  private Integer statusId;

  public StepResult() {
  }

  public String getContent() {
    return content;
  }

  public StepResult setContent(String content) {
    this.content = content;
    return this;
  }

  public String getExpected() {
    return expected;
  }

  public StepResult setExpected(String expected) {
    this.expected = expected;
    return this;
  }

  public String getActual() {
    return actual;
  }

  public StepResult setActual(String actual) {
    this.actual = actual;
    return this;
  }

  public Integer getStatusId() {
    return statusId;
  }

  public StepResult setStatusId(Integer statusId) {
    this.statusId = statusId;
    return this;
  }
}
