package dev.quokkify.testrail.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestRailUser {

  @JsonProperty("is_active")
  private Boolean isActive;
  private String name;
  private Integer id;
  private String email;

  public TestRailUser() {
  }

  public Boolean getActive() {
    return isActive;
  }

  public TestRailUser setActive(Boolean active) {
    isActive = active;
    return this;
  }

  public String getName() {
    return name;
  }

  public TestRailUser setName(String name) {
    this.name = name;
    return this;
  }

  public Integer getId() {
    return id;
  }

  public TestRailUser setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public TestRailUser setEmail(String email) {
    this.email = email;
    return this;
  }
}
