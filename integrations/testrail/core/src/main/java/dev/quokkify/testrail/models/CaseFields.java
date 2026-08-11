package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseFields {

  private List<ConfigsItem> configs;
  @JsonProperty("include_all")
  private boolean includeAll;
  @JsonProperty("is_active")
  private boolean isActive;
  @JsonProperty("type_id")
  private int typeId;
  @JsonProperty("system_name")
  private String systemName;
  private String name;
  @JsonProperty("display_order")
  private int displayOrder;
  private String description;
  private int id;
  private String label;
  @JsonProperty("template_ids")
  private List<Integer> templateIds;

  public CaseFields() {
  }

  public List<ConfigsItem> getConfigs() {
    return configs;
  }

  public CaseFields setConfigs(List<ConfigsItem> configs) {
    this.configs = configs;
    return this;
  }

  public boolean isIncludeAll() {
    return includeAll;
  }

  public CaseFields setIncludeAll(boolean includeAll) {
    this.includeAll = includeAll;
    return this;
  }

  public boolean isActive() {
    return isActive;
  }

  public CaseFields setActive(boolean active) {
    isActive = active;
    return this;
  }

  public int getTypeId() {
    return typeId;
  }

  public CaseFields setTypeId(int typeId) {
    this.typeId = typeId;
    return this;
  }

  public String getSystemName() {
    return systemName;
  }

  public CaseFields setSystemName(String systemName) {
    this.systemName = systemName;
    return this;
  }

  public String getName() {
    return name;
  }

  public CaseFields setName(String name) {
    this.name = name;
    return this;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public CaseFields setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public CaseFields setDescription(String description) {
    this.description = description;
    return this;
  }

  public int getId() {
    return id;
  }

  public CaseFields setId(int id) {
    this.id = id;
    return this;
  }

  public String getLabel() {
    return label;
  }

  public CaseFields setLabel(String label) {
    this.label = label;
    return this;
  }

  public List<Integer> getTemplateIds() {
    return templateIds;
  }

  public CaseFields setTemplateIds(List<Integer> templateIds) {
    this.templateIds = templateIds;
    return this;
  }
}
