package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCase {

  @JsonProperty("updated_on")
  private Integer updatedOn;
  @JsonProperty("custom_expected")
  private Object customExpected;
  @JsonProperty("custom_steps_separated")
  private List<CustomStepsSeparatedItem> customStepsSeparated;
  @JsonProperty("milestone_id")
  private Integer milestoneId;
  @JsonProperty("display_order")
  private Integer displayOrder;
  private String title;
  @JsonProperty("custom_steps")
  private Object customSteps;
  @JsonProperty("priority_id")
  private Integer priorityId;
  @JsonProperty("section_id")
  private Integer sectionId;
  private Object estimate;
  private Integer id;
  @JsonProperty("suite_id")
  private Integer suiteId;
  @JsonProperty("custom_mission")
  private String customMission;
  @JsonProperty("type_id")
  private Integer typeId;
  @JsonProperty("estimate_forecast")
  private Object estimateForecast;
  @JsonProperty("custom_preconds")
  private String customPreconds;
  @JsonProperty("custom_goals")
  private String customGoals;
  @JsonProperty("created_by")
  private Integer createdBy;
  @JsonProperty("custom_to_review")
  private Object customToReview;
  private Object refs;
  @JsonProperty("created_on")
  private Integer createdOn;
  @JsonProperty("updated_by")
  private Integer updatedBy;
  @JsonProperty("custom_test_type")
  private Object customTestType;
  @JsonProperty("template_id")
  private Integer templateId;
  @JsonProperty("custom_automation_type")
  private Integer customAutomationType;
  @JsonProperty("custom_tags")
  private List<Integer> customTags;

  public TestCase() {
  }

  public Integer getUpdatedOn() {
    return updatedOn;
  }

  public TestCase setUpdatedOn(Integer updatedOn) {
    this.updatedOn = updatedOn;
    return this;
  }

  public Object getCustomExpected() {
    return customExpected;
  }

  public TestCase setCustomExpected(Object customExpected) {
    this.customExpected = customExpected;
    return this;
  }

  public List<CustomStepsSeparatedItem> getCustomStepsSeparated() {
    return customStepsSeparated;
  }

  public TestCase setCustomStepsSeparated(List<CustomStepsSeparatedItem> customStepsSeparated) {
    this.customStepsSeparated = customStepsSeparated;
    return this;
  }

  public Integer getMilestoneId() {
    return milestoneId;
  }

  public TestCase setMilestoneId(Integer milestoneId) {
    this.milestoneId = milestoneId;
    return this;
  }

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public TestCase setDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public TestCase setTitle(String title) {
    this.title = title;
    return this;
  }

  public Object getCustomSteps() {
    return customSteps;
  }

  public TestCase setCustomSteps(Object customSteps) {
    this.customSteps = customSteps;
    return this;
  }

  public Integer getPriorityId() {
    return priorityId;
  }

  public TestCase setPriorityId(Integer priorityId) {
    this.priorityId = priorityId;
    return this;
  }

  public Integer getSectionId() {
    return sectionId;
  }

  public TestCase setSectionId(Integer sectionId) {
    this.sectionId = sectionId;
    return this;
  }

  public Object getEstimate() {
    return estimate;
  }

  public TestCase setEstimate(Object estimate) {
    this.estimate = estimate;
    return this;
  }

  public Integer getId() {
    return id;
  }

  public TestCase setId(Integer id) {
    this.id = id;
    return this;
  }

  public Integer getSuiteId() {
    return suiteId;
  }

  public TestCase setSuiteId(Integer suiteId) {
    this.suiteId = suiteId;
    return this;
  }

  public String getCustomMission() {
    return customMission;
  }

  public TestCase setCustomMission(String customMission) {
    this.customMission = customMission;
    return this;
  }

  public Integer getTypeId() {
    return typeId;
  }

  public TestCase setTypeId(Integer typeId) {
    this.typeId = typeId;
    return this;
  }

  public Object getEstimateForecast() {
    return estimateForecast;
  }

  public TestCase setEstimateForecast(Object estimateForecast) {
    this.estimateForecast = estimateForecast;
    return this;
  }

  public String getCustomPreconds() {
    return customPreconds;
  }

  public TestCase setCustomPreconds(String customPreconds) {
    this.customPreconds = customPreconds;
    return this;
  }

  public String getCustomGoals() {
    return customGoals;
  }

  public TestCase setCustomGoals(String customGoals) {
    this.customGoals = customGoals;
    return this;
  }

  public Integer getCreatedBy() {
    return createdBy;
  }

  public TestCase setCreatedBy(Integer createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public Object getCustomToReview() {
    return customToReview;
  }

  public TestCase setCustomToReview(Object customToReview) {
    this.customToReview = customToReview;
    return this;
  }

  public Object getRefs() {
    return refs;
  }

  public TestCase setRefs(Object refs) {
    this.refs = refs;
    return this;
  }

  public Integer getCreatedOn() {
    return createdOn;
  }

  public TestCase setCreatedOn(Integer createdOn) {
    this.createdOn = createdOn;
    return this;
  }

  public Integer getUpdatedBy() {
    return updatedBy;
  }

  public TestCase setUpdatedBy(Integer updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  public Object getCustomTestType() {
    return customTestType;
  }

  public TestCase setCustomTestType(Object customTestType) {
    this.customTestType = customTestType;
    return this;
  }

  public Integer getTemplateId() {
    return templateId;
  }

  public TestCase setTemplateId(Integer templateId) {
    this.templateId = templateId;
    return this;
  }

  public Integer getCustomAutomationType() {
    return customAutomationType;
  }

  public TestCase setCustomAutomationType(Integer customAutomationType) {
    this.customAutomationType = customAutomationType;
    return this;
  }

  public List<Integer> getCustomTags() {
    return customTags;
  }

  public TestCase setCustomTags(List<Integer> customTags) {
    this.customTags = customTags;
    return this;
  }
}
