package dev.quokkify.testrail.models;

public class CustomTag {

  private Integer id;
  private String name;

  public CustomTag() {
  }

  public CustomTag(Integer id, String name) {
    this.id = id;
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public CustomTag setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public CustomTag setName(String name) {
    this.name = name;
    return this;
  }
}
