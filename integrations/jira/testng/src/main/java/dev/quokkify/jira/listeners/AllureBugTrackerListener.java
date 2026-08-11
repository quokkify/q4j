package dev.quokkify.jira.listeners;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.constant.BugExecutionScope;
import dev.quokkify.jira.configs.JiraConfiguration;
import dev.quokkify.jira.services.JiraService;
import dev.quokkify.util.TestUtils;

import com.atlassian.jira.rest.client.api.domain.Issue;
import io.qameta.allure.Allure;
import io.qameta.allure.TmsLink;
import io.qameta.allure.model.Link;
import io.qameta.allure.util.ResultsUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestResult;

/**
 * Listener for adding links to Jira issues in Allure.
 */
public class AllureBugTrackerListener implements IInvokedMethodListener, ISuiteListener {

  private static final JiraConfiguration CONFIG = ConfigRegistry.get(JiraConfiguration.class);
  private Map<String, List<String>> disabledTestCases = Collections.emptyMap();

  @Override
  public void onStart(ISuite suite) {
    if (!isJiraEnabled()) {
      return;
    }
    if (bugExecutionScope().equals(BugExecutionScope.ALL_TESTS)) {
      JiraService jiraService = new JiraService(CONFIG.jiraUrl(), CONFIG.jiraToken());
      List<Issue> ticketsWithBug = jiraService.getIssues(CONFIG.jiraBugQuery());
      disabledTestCases = jiraService.getBugsWithTestCases(ticketsWithBug, CONFIG.jiraBugMarker());
    }
  }

  @Override
  public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
    if (!isJiraEnabled()) {
      return;
    }
    if (bugExecutionScope().equals(BugExecutionScope.ALL_TESTS)
        && testResult.getStatus() == ITestResult.FAILURE) {
      TmsLink testCaseIdAnnotation = TestUtils.getTestAnnotation(method.getTestMethod(), TmsLink.class);
      if (Objects.nonNull(testCaseIdAnnotation) && Objects.nonNull(testCaseIdAnnotation.value())) {
        disabledTestCases.entrySet().stream()
            .filter(map -> map.getValue().stream()
                .anyMatch(caseId -> caseId != null && caseId.trim().equals(testCaseIdAnnotation.value())))
            .map(Map.Entry::getKey)
            .toList()
            .forEach(ticketId -> {
              Link issue = ResultsUtils.createIssueLink(ticketId);
              Allure.issue(issue.getName(), issue.getUrl());
            });
      }
    }
  }

  private static BugExecutionScope bugExecutionScope() {
    return CONFIG.bugExecutionScope();
  }

  private static boolean isJiraEnabled() {
    return StringUtils.isNotBlank(CONFIG.jiraUrl())
        && StringUtils.isNotBlank(CONFIG.jiraToken())
        && StringUtils.isNotBlank(CONFIG.jiraBugQuery())
        && StringUtils.isNotBlank(CONFIG.jiraBugMarker());
  }
}
