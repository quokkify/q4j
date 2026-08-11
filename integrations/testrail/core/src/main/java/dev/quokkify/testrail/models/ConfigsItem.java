package dev.quokkify.testrail.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigsItem {

  private Context context;
  private Options options;
  private String id;

  public ConfigsItem() {
  }

  public Context getContext() {
    return context;
  }

  public ConfigsItem setContext(Context context) {
    this.context = context;
    return this;
  }

  public Options getOptions() {
    return options;
  }

  public ConfigsItem setOptions(Options options) {
    this.options = options;
    return this;
  }

  public String getId() {
    return id;
  }

  public ConfigsItem setId(String id) {
    this.id = id;
    return this;
  }
}
