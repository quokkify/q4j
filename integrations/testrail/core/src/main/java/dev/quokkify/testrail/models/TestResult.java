package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestResult {

  private Object elapsed;
  @JsonProperty("attachment_ids")
  private List<Object> attachmentIds;
  @JsonProperty("status_id")
  private Integer statusId;
  @JsonProperty("created_on")
  private Integer createdOn;
  private Object defects;
  @JsonProperty("assignedto_id")
  private Object assignedToId;
  private Object comment;
  private Integer id;
  @JsonProperty("created_by")
  private Integer createdBy;
  private Object version;
  @JsonProperty("custom_step_results")
  private List<StepResult> stepResults;
  @JsonProperty("test_id")
  private Integer testId;

  public TestResult() {
  }

  public Object getElapsed() {
    return elapsed;
  }

  public TestResult setElapsed(Object elapsed) {
    this.elapsed = elapsed;
    return this;
  }

  public List<Object> getAttachmentIds() {
    return attachmentIds;
  }

  public TestResult setAttachmentIds(List<Object> attachmentIds) {
    this.attachmentIds = attachmentIds;
    return this;
  }

  public Integer getStatusId() {
    return statusId;
  }

  public TestResult setStatusId(Integer statusId) {
    this.statusId = statusId;
    return this;
  }

  public Integer getCreatedOn() {
    return createdOn;
  }

  public TestResult setCreatedOn(Integer createdOn) {
    this.createdOn = createdOn;
    return this;
  }

  public Object getDefects() {
    return defects;
  }

  public TestResult setDefects(Object defects) {
    this.defects = defects;
    return this;
  }

  public Object getAssignedToId() {
    return assignedToId;
  }

  public TestResult setAssignedToId(Object assignedToId) {
    this.assignedToId = assignedToId;
    return this;
  }

  public Object getComment() {
    return comment;
  }

  public TestResult setComment(Object comment) {
    this.comment = comment;
    return this;
  }

  public Integer getId() {
    return id;
  }

  public TestResult setId(Integer id) {
    this.id = id;
    return this;
  }

  public Integer getCreatedBy() {
    return createdBy;
  }

  public TestResult setCreatedBy(Integer createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public Object getVersion() {
    return version;
  }

  public TestResult setVersion(Object version) {
    this.version = version;
    return this;
  }

  public List<StepResult> getStepResults() {
    return stepResults;
  }

  public TestResult setStepResults(List<StepResult> stepResults) {
    this.stepResults = stepResults;
    return this;
  }

  public Integer getTestId() {
    return testId;
  }

  public TestResult setTestId(Integer testId) {
    this.testId = testId;
    return this;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Object elapsed;
    private List<Object> attachmentIds;
    private Integer statusId;
    private Integer createdOn;
    private Object defects;
    private Object assignedToId;
    private Object comment;
    private Integer id;
    private Integer createdBy;
    private Object version;
    private List<StepResult> stepResults;
    private Integer testId;

    private Builder() {
    }

    public Builder elapsed(Object elapsed) {
      this.elapsed = elapsed;
      return this;
    }

    public Builder attachmentIds(List<Object> attachmentIds) {
      this.attachmentIds = attachmentIds;
      return this;
    }

    public Builder statusId(Integer statusId) {
      this.statusId = statusId;
      return this;
    }

    public Builder createdOn(Integer createdOn) {
      this.createdOn = createdOn;
      return this;
    }

    public Builder defects(Object defects) {
      this.defects = defects;
      return this;
    }

    public Builder assignedToId(Object assignedToId) {
      this.assignedToId = assignedToId;
      return this;
    }

    public Builder comment(Object comment) {
      this.comment = comment;
      return this;
    }

    public Builder id(Integer id) {
      this.id = id;
      return this;
    }

    public Builder createdBy(Integer createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder version(Object version) {
      this.version = version;
      return this;
    }

    public Builder stepResults(List<StepResult> stepResults) {
      this.stepResults = stepResults;
      return this;
    }

    public Builder testId(Integer testId) {
      this.testId = testId;
      return this;
    }

    public TestResult build() {
      TestResult result = new TestResult();
      result.setElapsed(elapsed);
      result.setAttachmentIds(attachmentIds);
      result.setStatusId(statusId);
      result.setCreatedOn(createdOn);
      result.setDefects(defects);
      result.setAssignedToId(assignedToId);
      result.setComment(comment);
      result.setId(id);
      result.setCreatedBy(createdBy);
      result.setVersion(version);
      result.setStepResults(stepResults);
      result.setTestId(testId);
      return result;
    }
  }
}
