package dev.quokkify.testrail.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Section {

  private Integer depth;
  private String description;
  @JsonProperty("display_order")
  private Integer displayOrder;
  private Integer id;
  private String name;
  @JsonProperty("parent_id")
  private Integer parentId;
  @JsonProperty("suite_id")
  private Integer suiteId;

  public Section() {
  }

  public Integer getDepth() {
    return depth;
  }

  public Section setDepth(Integer depth) {
    this.depth = depth;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Section setDescription(String description) {
    this.description = description;
    return this;
  }

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public Section setDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
    return this;
  }

  public Integer getId() {
    return id;
  }

  public Section setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public Section setName(String name) {
    this.name = name;
    return this;
  }

  public Integer getParentId() {
    return parentId;
  }

  public Section setParentId(Integer parentId) {
    this.parentId = parentId;
    return this;
  }

  public Integer getSuiteId() {
    return suiteId;
  }

  public Section setSuiteId(Integer suiteId) {
    this.suiteId = suiteId;
    return this;
  }
}
