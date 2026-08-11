package dev.quokkify.jira.configs;

import dev.quokkify.constant.BugExecutionScope;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:local_resources/jira.properties",
    "classpath:jira.properties"
})
public interface JiraConfiguration extends Config {

  @Key("JIRA_URL")
  String jiraUrl();

  @Key("JIRA_TOKEN")
  String jiraToken();

  @Key("JIRA_BUG_MARKER")
  String jiraBugMarker();

  @Key("JIRA_BUG_QUERY")
  String jiraBugQuery();

  @Key("BUG_EXECUTION_SCOPE")
  @DefaultValue("ALL_TESTS")
  BugExecutionScope bugExecutionScope();
}
