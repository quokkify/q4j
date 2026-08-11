package dev.quokkify.testrail.models;

public class CustomAutomationType {

  private Integer id;
  private String automationType;

  public CustomAutomationType() {
  }

  public CustomAutomationType(Integer id, String automationType) {
    this.id = id;
    this.automationType = automationType;
  }

  public Integer getId() {
    return id;
  }

  public CustomAutomationType setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getAutomationType() {
    return automationType;
  }

  public CustomAutomationType setAutomationType(String automationType) {
    this.automationType = automationType;
    return this;
  }
}
