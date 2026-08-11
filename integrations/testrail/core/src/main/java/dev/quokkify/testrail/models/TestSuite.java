package dev.quokkify.testrail.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestSuite {

  @JsonProperty("completed_on")
  private Long completedOn;
  private String description;
  private Integer id;
  @JsonProperty("is_baseline")
  private Boolean isBaseline;
  @JsonProperty("is_completed")
  private Boolean isCompleted;
  @JsonProperty("is_master")
  private Boolean isMaster;
  private String name;
  @JsonProperty("project_id")
  private Integer projectId;
  private String url;

  public TestSuite() {
  }

  public Long getCompletedOn() {
    return completedOn;
  }

  public TestSuite setCompletedOn(Long completedOn) {
    this.completedOn = completedOn;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public TestSuite setDescription(String description) {
    this.description = description;
    return this;
  }

  public Integer getId() {
    return id;
  }

  public TestSuite setId(Integer id) {
    this.id = id;
    return this;
  }

  public Boolean getBaseline() {
    return isBaseline;
  }

  public TestSuite setBaseline(Boolean baseline) {
    isBaseline = baseline;
    return this;
  }

  public Boolean getCompleted() {
    return isCompleted;
  }

  public TestSuite setCompleted(Boolean completed) {
    isCompleted = completed;
    return this;
  }

  public Boolean getMaster() {
    return isMaster;
  }

  public TestSuite setMaster(Boolean master) {
    isMaster = master;
    return this;
  }

  public String getName() {
    return name;
  }

  public TestSuite setName(String name) {
    this.name = name;
    return this;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public TestSuite setProjectId(Integer projectId) {
    this.projectId = projectId;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public TestSuite setUrl(String url) {
    this.url = url;
    return this;
  }
}
