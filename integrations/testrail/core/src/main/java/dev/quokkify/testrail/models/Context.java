package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Context {

  @JsonProperty("is_global")
  private boolean isGlobal;
  @JsonProperty("project_ids")
  private List<Integer> projectIds;

  public Context() {
  }

  public boolean isGlobal() {
    return isGlobal;
  }

  public Context setGlobal(boolean global) {
    isGlobal = global;
    return this;
  }

  public List<Integer> getProjectIds() {
    return projectIds;
  }

  public Context setProjectIds(List<Integer> projectIds) {
    this.projectIds = projectIds;
    return this;
  }
}
