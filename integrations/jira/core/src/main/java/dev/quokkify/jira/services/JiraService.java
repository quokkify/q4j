package dev.quokkify.jira.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.quokkify.constant.StringConstant;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Jira client service for executing JQL queries.
 */
public class JiraService {

  private static final Logger LOG = LogManager.getLogger(JiraService.class);
  private final String jiraUrl;
  private final String token;

  public JiraService(String jiraUrl, String token) {
    this.jiraUrl = jiraUrl;
    this.token = token;
  }

  private URI getJiraUri() {
    try {
      return new URI(jiraUrl);
    } catch (URISyntaxException exception) {
      throw new IllegalArgumentException("Jira URL is invalid: " + jiraUrl, exception);
    }
  }

  private JiraRestClient getJiraRestClient() {
    return new AsynchronousJiraRestClientFactory()
        .createWithAuthenticationHandler(getJiraUri(), new BearerHandler(token));
  }

  /**
   * Get list of issues from Jira using JQL.
   *
   * @param jql jira query
   * @return issue list
   */
  public List<Issue> getIssues(String jql) {
    String allFieldsMarker = "*all";
    Iterable<Issue> iterator;
    try (JiraRestClient jiraRestClient = getJiraRestClient()) {
      iterator = jiraRestClient.getSearchClient()
          .searchJql(jql, -1, 0, new HashSet<>(List.of(allFieldsMarker))).claim().getIssues();
    } catch (Exception exception) {
      LOG.error("Jira service does not active!!! Query '{}' is not executed", jql, exception);
      return Collections.emptyList();
    }
    return StreamSupport.stream(iterator.spliterator(), false).toList();
  }

  /**
   * Get Map with key - test case id and values jira ticket id.
   *
   * @param issues list of issues from jira
   * @param marker marker if bug in comment (set in property file)
   * @return Map of ticketIds with testcaseIds
   */
  public Map<String, List<String>> getTestCasesWithBugs(List<Issue> issues, String marker) {
    return distinctMap(invertMap(getBugsWithTestCases(issues, marker)));
  }

  /**
   * Get Map with key - jira ticket id and values test case id.
   *
   * @param issues list of issues from jira
   * @param marker marker if bug in comment (set in property file)
   * @return Map of ticketIds with testcaseIds
   */
  public Map<String, List<String>> getBugsWithTestCases(List<Issue> issues, String marker) {
    return issues.stream().collect(Collectors.toMap(
        BasicIssue::getKey, issue -> StreamSupport.stream(issue.getComments().spliterator(), false)
            .map(Comment::getBody)
            .filter(comment -> comment.contains(marker))
            .map(comment ->
                Arrays.asList(comment.substring(comment.indexOf(marker) + marker.length())
                    .split(StringConstant.COMMA)))
            .flatMap(Collection::stream).distinct().map(String::trim).toList()));
  }

  private Map<String, List<String>> invertMap(Map<String, List<String>> source) {
    Map<String, List<String>> inverted = new HashMap<>();
    source.forEach((key, values) -> values.forEach(value -> {
      if (value == null) {
        return;
      }
      inverted.computeIfAbsent(value, entry -> new ArrayList<>()).add(key);
    }));
    return inverted;
  }

  private Map<String, List<String>> distinctMap(Map<String, List<String>> source) {
    return source.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList()));
  }
}
