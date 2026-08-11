package dev.quokkify.jira.listeners;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.constant.BugExecutionScope;
import dev.quokkify.constant.StringConstant;
import dev.quokkify.jira.configs.JiraConfiguration;
import dev.quokkify.jira.services.JiraService;
import dev.quokkify.util.TestUtils;

import com.atlassian.jira.rest.client.api.domain.Issue;
import io.qameta.allure.TmsLink;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IExecutionListener;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

/**
 * Listener for skipping tests which are marked as bugs in Jira tickets.
 */
public class BugTrackerListener implements IMethodInterceptor, IExecutionListener {

  private static final Logger LOG = LogManager.getLogger(BugTrackerListener.class);
  private static final JiraConfiguration CONFIG = ConfigRegistry.get(JiraConfiguration.class);
  private List<String> disabledTestCases = Collections.emptyList();

  @Override
  public void onExecutionStart() {
    if (!isJiraEnabled()) {
      return;
    }
    if (!bugExecutionScope().equals(BugExecutionScope.ALL_TESTS)) {
      JiraService jiraService = new JiraService(CONFIG.jiraUrl(), CONFIG.jiraToken());
      List<Issue> ticketsWithBug = jiraService.getIssues(CONFIG.jiraBugQuery());
      disabledTestCases =
          jiraService.getBugsWithTestCases(ticketsWithBug, CONFIG.jiraBugMarker()).values().stream()
              .flatMap(Collection::stream).distinct().toList();
      LOG.info("Collected disabled test cases via Jira: '{}'",
          String.join(StringConstant.COMMA_SPACE, disabledTestCases));
    }
  }

  @Override
  public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
    List<IMethodInstance> methodInstancesList = methods.stream().filter(getBugExecutionScopePredicate()).toList();
    String resultMethods = methodInstancesList.stream()
        .filter(methodInstances ->
            Objects.nonNull(TestUtils.getTestAnnotation(methodInstances.getMethod(), TmsLink.class)))
        .map(methodInstances ->
            TestUtils.getTestAnnotation(methodInstances.getMethod(), TmsLink.class).value())
        .collect(Collectors.joining(StringConstant.COMMA_SPACE)).trim();
    if (!resultMethods.isEmpty()) {
      LOG.debug("Test cases for execute: {}", resultMethods);
    }
    return methodInstancesList;
  }

  private boolean isTestEnabled(TmsLink testCaseIdAnnotation) {
    return Objects.nonNull(testCaseIdAnnotation)
        && Objects.nonNull(testCaseIdAnnotation.value())
        && disabledTestCases.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(string -> !string.isBlank())
        .noneMatch(caseId -> caseId.equals(testCaseIdAnnotation.value()));
  }

  private Predicate<IMethodInstance> getBugExecutionScopePredicate() {
    if (!isJiraEnabled()) {
      return methodInstance -> true;
    }
    return switch (bugExecutionScope()) {
      case EXCLUDE_TESTS_WITH_BUGS -> methodInstance -> {
        TmsLink testCaseIdAnnotation = TestUtils.getTestAnnotation(methodInstance.getMethod(), TmsLink.class);
        return Objects.isNull(testCaseIdAnnotation)  // for Test Data Setup, when test has no TmsLink annotation
            || isTestEnabled(testCaseIdAnnotation);
      };
      case TESTS_WITH_BUGS -> methodInstance ->
          !isTestEnabled(TestUtils.getTestAnnotation(methodInstance.getMethod(), TmsLink.class));
      case ALL_TESTS -> methodInstance -> true;
    };
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
