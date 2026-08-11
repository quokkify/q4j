package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Entry {

  @JsonProperty("suite_id")
  private Long suiteId;
  private String description;
  private String id;
  private String name;
  @JsonProperty("include_all")
  private Boolean includeAll;
  private Object refs;
  private List<TestRun> runs;

  public Entry() {
  }

  public Long getSuiteId() {
    return suiteId;
  }

  public Entry setSuiteId(Long suiteId) {
    this.suiteId = suiteId;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Entry setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getId() {
    return id;
  }

  public Entry setId(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public Entry setName(String name) {
    this.name = name;
    return this;
  }

  public Boolean getIncludeAll() {
    return includeAll;
  }

  public Entry setIncludeAll(Boolean includeAll) {
    this.includeAll = includeAll;
    return this;
  }

  public Object getRefs() {
    return refs;
  }

  public Entry setRefs(Object refs) {
    this.refs = refs;
    return this;
  }

  public List<TestRun> getRuns() {
    return runs;
  }

  public Entry setRuns(List<TestRun> runs) {
    this.runs = runs;
    return this;
  }
}
