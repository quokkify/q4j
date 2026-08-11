package dev.quokkify.testrail.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Sections {

  private Integer offset;
  private Integer limit;
  private Integer size;
  @JsonProperty("_links")
  private Object links;
  private List<Section> sections;

  public Sections() {
  }

  public Integer getOffset() {
    return offset;
  }

  public Sections setOffset(Integer offset) {
    this.offset = offset;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public Sections setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public Integer getSize() {
    return size;
  }

  public Sections setSize(Integer size) {
    this.size = size;
    return this;
  }

  public Object getLinks() {
    return links;
  }

  public Sections setLinks(Object links) {
    this.links = links;
    return this;
  }

  public List<Section> getSections() {
    return sections;
  }

  public Sections setSections(List<Section> sections) {
    this.sections = sections;
    return this;
  }
}
