package dev.quokkify.testrail.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCaseType {

  private String name;
  private Integer id;
  @JsonProperty("is_default")
  private Boolean isDefault;

  public TestCaseType() {
  }

  public String getName() {
    return name;
  }

  public TestCaseType setName(String name) {
    this.name = name;
    return this;
  }

  public Integer getId() {
    return id;
  }

  public TestCaseType setId(Integer id) {
    this.id = id;
    return this;
  }

  public Boolean getDefault() {
    return isDefault;
  }

  public TestCaseType setDefault(Boolean isDefault) {
    this.isDefault = isDefault;
    return this;
  }
}
