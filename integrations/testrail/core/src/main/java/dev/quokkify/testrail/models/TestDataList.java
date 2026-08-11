package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestDataList {

  private Object offset;
  private Object limit;
  private Object size;
  @JsonProperty("_links")
  private Object links;
  private List<TestData> tests;

  public TestDataList() {
  }

  public Object getOffset() {
    return offset;
  }

  public TestDataList setOffset(Object offset) {
    this.offset = offset;
    return this;
  }

  public Object getLimit() {
    return limit;
  }

  public TestDataList setLimit(Object limit) {
    this.limit = limit;
    return this;
  }

  public Object getSize() {
    return size;
  }

  public TestDataList setSize(Object size) {
    this.size = size;
    return this;
  }

  public Object getLinks() {
    return links;
  }

  public TestDataList setLinks(Object links) {
    this.links = links;
    return this;
  }

  public List<TestData> getTests() {
    return tests;
  }

  public TestDataList setTests(List<TestData> tests) {
    this.tests = tests;
    return this;
  }
}
