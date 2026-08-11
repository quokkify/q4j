package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestPlan {

  @JsonProperty("completed_on")
  private Long completedOn;
  private String description;
  private Integer id;
  @JsonProperty("is_completed")
  private Boolean isCompleted;
  private String name;
  @JsonProperty("project_id")
  private Integer projectId;
  private String url;
  private List<Entry> entries;

  public TestPlan() {
  }

  public Long getCompletedOn() {
    return completedOn;
  }

  public TestPlan setCompletedOn(Long completedOn) {
    this.completedOn = completedOn;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public TestPlan setDescription(String description) {
    this.description = description;
    return this;
  }

  public Integer getId() {
    return id;
  }

  public TestPlan setId(Integer id) {
    this.id = id;
    return this;
  }

  public Boolean getCompleted() {
    return isCompleted;
  }

  public TestPlan setCompleted(Boolean completed) {
    isCompleted = completed;
    return this;
  }

  public String getName() {
    return name;
  }

  public TestPlan setName(String name) {
    this.name = name;
    return this;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public TestPlan setProjectId(Integer projectId) {
    this.projectId = projectId;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public TestPlan setUrl(String url) {
    this.url = url;
    return this;
  }

  public List<Entry> getEntries() {
    return entries;
  }

  public TestPlan setEntries(List<Entry> entries) {
    this.entries = entries;
    return this;
  }
}
