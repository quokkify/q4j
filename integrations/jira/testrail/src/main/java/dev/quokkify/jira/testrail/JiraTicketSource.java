package dev.quokkify.jira.testrail;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.jira.configs.JiraConfiguration;
import dev.quokkify.jira.services.JiraService;
import dev.quokkify.testrail.tickets.TicketSource;

import com.atlassian.jira.rest.client.api.domain.Issue;
import org.apache.commons.lang3.StringUtils;

public class JiraTicketSource implements TicketSource {

  private static final JiraConfiguration CONFIG = ConfigRegistry.get(JiraConfiguration.class);

  @Override
  public boolean isEnabled() {
    return StringUtils.isNotBlank(CONFIG.jiraUrl())
        && StringUtils.isNotBlank(CONFIG.jiraToken())
        && StringUtils.isNotBlank(CONFIG.jiraBugQuery())
        && StringUtils.isNotBlank(CONFIG.jiraBugMarker());
  }

  @Override
  public Map<String, List<String>> getTestCasesWithBugs() {
    if (!isEnabled()) {
      return Collections.emptyMap();
    }
    JiraService jiraService = new JiraService(CONFIG.jiraUrl(), CONFIG.jiraToken());
    List<Issue> ticketsWithBug = jiraService.getIssues(CONFIG.jiraBugQuery());
    return jiraService.getTestCasesWithBugs(ticketsWithBug, CONFIG.jiraBugMarker());
  }

  @Override
  public String buildTicketLink(String ticketId) {
    return "[%2$s](%1$s%2$s)".formatted(jiraIssueUrl(), ticketId);
  }

  @Override
  public String label() {
    return "Jira Issue(s)";
  }

  private static String jiraIssueUrl() {
    return StringUtils.appendIfMissing(CONFIG.jiraUrl(), "/") + "browse/";
  }
}
