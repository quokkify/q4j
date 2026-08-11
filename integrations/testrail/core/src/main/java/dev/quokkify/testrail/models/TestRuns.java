package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestRuns {

  private Object offset;
  private Object limit;
  private Object size;
  @JsonProperty("_links")
  private Object links;
  private List<TestRun> runs;

  public TestRuns() {
  }

  public Object getOffset() {
    return offset;
  }

  public TestRuns setOffset(Object offset) {
    this.offset = offset;
    return this;
  }

  public Object getLimit() {
    return limit;
  }

  public TestRuns setLimit(Object limit) {
    this.limit = limit;
    return this;
  }

  public Object getSize() {
    return size;
  }

  public TestRuns setSize(Object size) {
    this.size = size;
    return this;
  }

  public Object getLinks() {
    return links;
  }

  public TestRuns setLinks(Object links) {
    this.links = links;
    return this;
  }

  public List<TestRun> getRuns() {
    return runs;
  }

  public TestRuns setRuns(List<TestRun> runs) {
    this.runs = runs;
    return this;
  }
}
