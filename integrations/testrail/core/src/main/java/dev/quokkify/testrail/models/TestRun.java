package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestRun {

  @JsonProperty("case_ids")
  private List<Integer> casesIds;
  @JsonProperty("completed_on")
  private Integer completedOn;
  @JsonProperty("milestone_id")
  private Integer milestoneId;
  private String description;
  @JsonProperty("custom_status3_count")
  private Integer customStatus3Count;
  @JsonProperty("is_completed")
  private Boolean isCompleted;
  @JsonProperty("retest_count")
  private Integer retestCount;
  @JsonProperty("custom_status5_count")
  private Integer customStatus5Count;
  @JsonProperty("project_id")
  private Integer projectId;
  private Integer id;
  @JsonProperty("suite_id")
  private Integer suiteId;
  @JsonProperty("custom_status2_count")
  private Integer customStatus2Count;
  @JsonProperty("include_all")
  private Boolean includeAll;
  @JsonProperty("passed_count")
  private Integer passedCount;
  @JsonProperty("custom_status7_count")
  private Integer customStatus7Count;
  @JsonProperty("custom_status4_count")
  private Integer customStatus4Count;
  @JsonProperty("created_by")
  private Integer createdBy;
  private String url;
  @JsonProperty("config_ids")
  private List<Object> configIds;
  @JsonProperty("blocked_count")
  private Integer blockedCount;
  @JsonProperty("created_on")
  private Integer createdOn;
  private String refs;
  @JsonProperty("untested_count")
  private Integer untestedCount;
  private String name;
  @JsonProperty("assignedto_id")
  private Integer assignedToId;
  @JsonProperty("failed_count")
  private Integer failedCount;
  @JsonProperty("custom_status1_count")
  private Integer customStatus1Count;
  @JsonProperty("custom_status6_count")
  private Integer customStatus6Count;
  private Object config;
  @JsonProperty("plan_id")
  private Integer planId;

  public TestRun() {
  }

  public List<Integer> getCasesIds() {
    return casesIds;
  }

  public TestRun setCasesIds(List<Integer> casesIds) {
    this.casesIds = casesIds;
    return this;
  }

  public Integer getCompletedOn() {
    return completedOn;
  }

  public TestRun setCompletedOn(Integer completedOn) {
    this.completedOn = completedOn;
    return this;
  }

  public Integer getMilestoneId() {
    return milestoneId;
  }

  public TestRun setMilestoneId(Integer milestoneId) {
    this.milestoneId = milestoneId;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public TestRun setDescription(String description) {
    this.description = description;
    return this;
  }

  public Integer getCustomStatus3Count() {
    return customStatus3Count;
  }

  public TestRun setCustomStatus3Count(Integer customStatus3Count) {
    this.customStatus3Count = customStatus3Count;
    return this;
  }

  public Boolean getCompleted() {
    return isCompleted;
  }

  public TestRun setCompleted(Boolean completed) {
    isCompleted = completed;
    return this;
  }

  public Integer getRetestCount() {
    return retestCount;
  }

  public TestRun setRetestCount(Integer retestCount) {
    this.retestCount = retestCount;
    return this;
  }

  public Integer getCustomStatus5Count() {
    return customStatus5Count;
  }

  public TestRun setCustomStatus5Count(Integer customStatus5Count) {
    this.customStatus5Count = customStatus5Count;
    return this;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public TestRun setProjectId(Integer projectId) {
    this.projectId = projectId;
    return this;
  }

  public Integer getId() {
    return id;
  }

  public TestRun setId(Integer id) {
    this.id = id;
    return this;
  }

  public Integer getSuiteId() {
    return suiteId;
  }

  public TestRun setSuiteId(Integer suiteId) {
    this.suiteId = suiteId;
    return this;
  }

  public Integer getCustomStatus2Count() {
    return customStatus2Count;
  }

  public TestRun setCustomStatus2Count(Integer customStatus2Count) {
    this.customStatus2Count = customStatus2Count;
    return this;
  }

  public Boolean getIncludeAll() {
    return includeAll;
  }

  public TestRun setIncludeAll(Boolean includeAll) {
    this.includeAll = includeAll;
    return this;
  }

  public Integer getPassedCount() {
    return passedCount;
  }

  public TestRun setPassedCount(Integer passedCount) {
    this.passedCount = passedCount;
    return this;
  }

  public Integer getCustomStatus7Count() {
    return customStatus7Count;
  }

  public TestRun setCustomStatus7Count(Integer customStatus7Count) {
    this.customStatus7Count = customStatus7Count;
    return this;
  }

  public Integer getCustomStatus4Count() {
    return customStatus4Count;
  }

  public TestRun setCustomStatus4Count(Integer customStatus4Count) {
    this.customStatus4Count = customStatus4Count;
    return this;
  }

  public Integer getCreatedBy() {
    return createdBy;
  }

  public TestRun setCreatedBy(Integer createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public TestRun setUrl(String url) {
    this.url = url;
    return this;
  }

  public List<Object> getConfigIds() {
    return configIds;
  }

  public TestRun setConfigIds(List<Object> configIds) {
    this.configIds = configIds;
    return this;
  }

  public Integer getBlockedCount() {
    return blockedCount;
  }

  public TestRun setBlockedCount(Integer blockedCount) {
    this.blockedCount = blockedCount;
    return this;
  }

  public Integer getCreatedOn() {
    return createdOn;
  }

  public TestRun setCreatedOn(Integer createdOn) {
    this.createdOn = createdOn;
    return this;
  }

  public String getRefs() {
    return refs;
  }

  public TestRun setRefs(String refs) {
    this.refs = refs;
    return this;
  }

  public Integer getUntestedCount() {
    return untestedCount;
  }

  public TestRun setUntestedCount(Integer untestedCount) {
    this.untestedCount = untestedCount;
    return this;
  }

  public String getName() {
    return name;
  }

  public TestRun setName(String name) {
    this.name = name;
    return this;
  }

  public Integer getAssignedToId() {
    return assignedToId;
  }

  public TestRun setAssignedToId(Integer assignedToId) {
    this.assignedToId = assignedToId;
    return this;
  }

  public Integer getFailedCount() {
    return failedCount;
  }

  public TestRun setFailedCount(Integer failedCount) {
    this.failedCount = failedCount;
    return this;
  }

  public Integer getCustomStatus1Count() {
    return customStatus1Count;
  }

  public TestRun setCustomStatus1Count(Integer customStatus1Count) {
    this.customStatus1Count = customStatus1Count;
    return this;
  }

  public Integer getCustomStatus6Count() {
    return customStatus6Count;
  }

  public TestRun setCustomStatus6Count(Integer customStatus6Count) {
    this.customStatus6Count = customStatus6Count;
    return this;
  }

  public Object getConfig() {
    return config;
  }

  public TestRun setConfig(Object config) {
    this.config = config;
    return this;
  }

  public Integer getPlanId() {
    return planId;
  }

  public TestRun setPlanId(Integer planId) {
    this.planId = planId;
    return this;
  }
}
