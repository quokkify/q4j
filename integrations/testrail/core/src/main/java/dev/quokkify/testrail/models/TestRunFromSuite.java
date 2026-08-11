package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestRunFromSuite {

  @JsonProperty("suite_id")
  private Integer suiteId;
  private String name;
  @JsonProperty("milestone_id")
  private Integer milestoneId;
  private String description;
  @JsonProperty("assignedto_id")
  private Integer assignedToId;
  @JsonProperty("include_all")
  private Boolean includeAll = Boolean.TRUE;
  @JsonProperty("case_ids")
  private List<Integer> casesIds;

  public TestRunFromSuite() {
  }

  public Integer getSuiteId() {
    return suiteId;
  }

  public TestRunFromSuite setSuiteId(Integer suiteId) {
    this.suiteId = suiteId;
    return this;
  }

  public String getName() {
    return name;
  }

  public TestRunFromSuite setName(String name) {
    this.name = name;
    return this;
  }

  public Integer getMilestoneId() {
    return milestoneId;
  }

  public TestRunFromSuite setMilestoneId(Integer milestoneId) {
    this.milestoneId = milestoneId;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public TestRunFromSuite setDescription(String description) {
    this.description = description;
    return this;
  }

  public Integer getAssignedToId() {
    return assignedToId;
  }

  public TestRunFromSuite setAssignedToId(Integer assignedToId) {
    this.assignedToId = assignedToId;
    return this;
  }

  public Boolean getIncludeAll() {
    return includeAll;
  }

  public TestRunFromSuite setIncludeAll(Boolean includeAll) {
    this.includeAll = includeAll;
    return this;
  }

  public List<Integer> getCasesIds() {
    return casesIds;
  }

  public TestRunFromSuite setCasesIds(List<Integer> casesIds) {
    this.casesIds = casesIds;
    return this;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Integer suiteId;
    private String name;
    private Integer milestoneId;
    private String description;
    private Integer assignedToId;
    private Boolean includeAll = Boolean.TRUE;
    private List<Integer> casesIds;

    private Builder() {
    }

    public Builder suiteId(Integer suiteId) {
      this.suiteId = suiteId;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder milestoneId(Integer milestoneId) {
      this.milestoneId = milestoneId;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder assignedToId(Integer assignedToId) {
      this.assignedToId = assignedToId;
      return this;
    }

    public Builder includeAll(Boolean includeAll) {
      this.includeAll = includeAll;
      return this;
    }

    public Builder casesIds(List<Integer> casesIds) {
      this.casesIds = casesIds;
      return this;
    }

    public TestRunFromSuite build() {
      TestRunFromSuite result = new TestRunFromSuite();
      result.setSuiteId(suiteId);
      result.setName(name);
      result.setMilestoneId(milestoneId);
      result.setDescription(description);
      result.setAssignedToId(assignedToId);
      result.setIncludeAll(includeAll);
      result.setCasesIds(casesIds);
      return result;
    }
  }
}
