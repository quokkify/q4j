package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestData {

  private Integer id;
  @JsonProperty("case_id")
  private Integer caseId;
  @JsonProperty("status_id")
  private Integer statusId;
  @JsonProperty("assignedto_id")
  private Integer assignedToId;
  @JsonProperty("run_id")
  private Integer runId;
  private String title;
  @JsonProperty("template_id")
  private Integer templateId;
  @JsonProperty("type_id")
  private Integer typeId;
  @JsonProperty("priority_id")
  private Integer priorityId;
  private Object estimate;
  @JsonProperty("estimate_forecast")
  private Object estimateForecast;
  private Object refs;
  @JsonProperty("milestone_id")
  private Object milestoneId;
  @JsonProperty("custom_automation_type")
  private Integer customAutomationType;
  @JsonProperty("custom_test_type")
  private Integer customTestType;
  @JsonProperty("custom_to_review")
  private Object customToReview;
  @JsonProperty("custom_preconds")
  private Object customPreconds;
  @JsonProperty("custom_steps")
  private Object customSteps;
  @JsonProperty("custom_expected")
  private Object customExpected;
  @JsonProperty("custom_steps_separated")
  private Object customStepsSeparated;
  @JsonProperty("custom_mission")
  private Object customMission;
  @JsonProperty("custom_goals")
  private Object customGoals;
  @JsonProperty("custom_tags")
  private List<Integer> customTags = null;

  public TestData() {
  }

  public Integer getId() {
    return id;
  }

  public TestData setId(Integer id) {
    this.id = id;
    return this;
  }

  public Integer getCaseId() {
    return caseId;
  }

  public TestData setCaseId(Integer caseId) {
    this.caseId = caseId;
    return this;
  }

  public Integer getStatusId() {
    return statusId;
  }

  public TestData setStatusId(Integer statusId) {
    this.statusId = statusId;
    return this;
  }

  public Integer getAssignedToId() {
    return assignedToId;
  }

  public TestData setAssignedToId(Integer assignedToId) {
    this.assignedToId = assignedToId;
    return this;
  }

  public Integer getRunId() {
    return runId;
  }

  public TestData setRunId(Integer runId) {
    this.runId = runId;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public TestData setTitle(String title) {
    this.title = title;
    return this;
  }

  public Integer getTemplateId() {
    return templateId;
  }

  public TestData setTemplateId(Integer templateId) {
    this.templateId = templateId;
    return this;
  }

  public Integer getTypeId() {
    return typeId;
  }

  public TestData setTypeId(Integer typeId) {
    this.typeId = typeId;
    return this;
  }

  public Integer getPriorityId() {
    return priorityId;
  }

  public TestData setPriorityId(Integer priorityId) {
    this.priorityId = priorityId;
    return this;
  }

  public Object getEstimate() {
    return estimate;
  }

  public TestData setEstimate(Object estimate) {
    this.estimate = estimate;
    return this;
  }

  public Object getEstimateForecast() {
    return estimateForecast;
  }

  public TestData setEstimateForecast(Object estimateForecast) {
    this.estimateForecast = estimateForecast;
    return this;
  }

  public Object getRefs() {
    return refs;
  }

  public TestData setRefs(Object refs) {
    this.refs = refs;
    return this;
  }

  public Object getMilestoneId() {
    return milestoneId;
  }

  public TestData setMilestoneId(Object milestoneId) {
    this.milestoneId = milestoneId;
    return this;
  }

  public Integer getCustomAutomationType() {
    return customAutomationType;
  }

  public TestData setCustomAutomationType(Integer customAutomationType) {
    this.customAutomationType = customAutomationType;
    return this;
  }

  public Integer getCustomTestType() {
    return customTestType;
  }

  public TestData setCustomTestType(Integer customTestType) {
    this.customTestType = customTestType;
    return this;
  }

  public Object getCustomToReview() {
    return customToReview;
  }

  public TestData setCustomToReview(Object customToReview) {
    this.customToReview = customToReview;
    return this;
  }

  public Object getCustomPreconds() {
    return customPreconds;
  }

  public TestData setCustomPreconds(Object customPreconds) {
    this.customPreconds = customPreconds;
    return this;
  }

  public Object getCustomSteps() {
    return customSteps;
  }

  public TestData setCustomSteps(Object customSteps) {
    this.customSteps = customSteps;
    return this;
  }

  public Object getCustomExpected() {
    return customExpected;
  }

  public TestData setCustomExpected(Object customExpected) {
    this.customExpected = customExpected;
    return this;
  }

  public Object getCustomStepsSeparated() {
    return customStepsSeparated;
  }

  public TestData setCustomStepsSeparated(Object customStepsSeparated) {
    this.customStepsSeparated = customStepsSeparated;
    return this;
  }

  public Object getCustomMission() {
    return customMission;
  }

  public TestData setCustomMission(Object customMission) {
    this.customMission = customMission;
    return this;
  }

  public Object getCustomGoals() {
    return customGoals;
  }

  public TestData setCustomGoals(Object customGoals) {
    this.customGoals = customGoals;
    return this;
  }

  public List<Integer> getCustomTags() {
    return customTags;
  }

  public TestData setCustomTags(List<Integer> customTags) {
    this.customTags = customTags;
    return this;
  }
}
