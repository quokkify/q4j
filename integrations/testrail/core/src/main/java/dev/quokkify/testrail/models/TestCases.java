package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCases {

  private Object offset;
  private Object limit;
  private Object size;
  @JsonProperty("_links")
  private Object links;
  private List<TestCase> cases;

  public TestCases() {
  }

  public Object getOffset() {
    return offset;
  }

  public TestCases setOffset(Object offset) {
    this.offset = offset;
    return this;
  }

  public Object getLimit() {
    return limit;
  }

  public TestCases setLimit(Object limit) {
    this.limit = limit;
    return this;
  }

  public Object getSize() {
    return size;
  }

  public TestCases setSize(Object size) {
    this.size = size;
    return this;
  }

  public Object getLinks() {
    return links;
  }

  public TestCases setLinks(Object links) {
    this.links = links;
    return this;
  }

  public List<TestCase> getCases() {
    return cases;
  }

  public TestCases setCases(List<TestCase> cases) {
    this.cases = cases;
    return this;
  }
}
