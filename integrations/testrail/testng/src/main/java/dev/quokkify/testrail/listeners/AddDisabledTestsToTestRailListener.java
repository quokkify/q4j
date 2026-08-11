package dev.quokkify.testrail.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.constant.StringConstant;
import dev.quokkify.testrail.configs.TestRailConfiguration;
import dev.quokkify.testrail.tickets.TicketSource;
import dev.quokkify.testrail.utils.TestRailHelper;
import dev.quokkify.testrail.utils.TestRailTestFilterRules;
import dev.quokkify.util.TestUtils;

import io.qameta.allure.TmsLink;
import org.apache.commons.lang3.StringUtils;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestNGMethod;

/**
 * Listener adds info (description, issue link) about disabled tests to TestRail.
 * Add only to tests filtered by rules from {@link TestRailTestFilterRules}
 */
public class AddDisabledTestsToTestRailListener implements ISuiteListener {

  private static final TestRailConfiguration CONFIG = ConfigRegistry.get(TestRailConfiguration.class);
  private static final boolean IS_TESTRAIL_ENABLED =
      !CONFIG.isTestrailDisabled() && StringUtils.isNotEmpty(CONFIG.testRailId());

  @Override
  public void onFinish(ISuite suite) {
    if (!IS_TESTRAIL_ENABLED) return;
    List<ITestNGMethod> excludedMethods = new ArrayList<>(suite.getExcludedMethods());
    setDisabledTestsAsFailedToTestRail(excludedMethods);
    addTicketBugsToTestRail(excludedMethods);
    if (CONFIG.closeTestRun()) {
      TestRailHelper.closeActualTestRuns();
    }
    if (CONFIG.deleteTestRun()) {
      TestRailHelper.deleteActualTestRuns();
    }
  }

  private void setDisabledTestsAsFailedToTestRail(List<ITestNGMethod> allTests) {
    allTests.stream()
        .filter(test -> Objects.nonNull(TestUtils.getTestAnnotation(test, TmsLink.class)))
        .filter(test -> !test.getEnabled())
        .forEach(test -> {
          AtomicReference<String> commentMessage =
              new AtomicReference<>("TC-%s is disabled via 'enabled' attribute.".formatted(getTestCaseId(test)));
          String testCaseId = getTestCaseId(test);
          if (TestRailTestFilterRules.needRunTest(testCaseId)) {
            TestRailHelper.addTestResultForDisabledTest(
                testCaseId,
                CONFIG.userIdAssignedDisabledTests(),
                commentMessage.get());
          }
        });
  }

  private void addTicketBugsToTestRail(List<ITestNGMethod> excludedTests) {
    List<TicketSource> sources = loadTicketSources();
    if (sources.isEmpty()) {
      return;
    }
    Map<String, String> commentsByTestCase = buildCommentsByTestCase(sources);
    if (commentsByTestCase.isEmpty()) {
      return;
    }
    excludedTests.stream()
        .filter(test -> Objects.nonNull(TestUtils.getTestAnnotation(test, TmsLink.class)))
        .filter(test -> commentsByTestCase.containsKey(getTestCaseId(test)))
        .forEach(test -> {
          String testCaseId = getTestCaseId(test);
          AtomicReference<String> commentMessage =
              new AtomicReference<>(commentsByTestCase.get(testCaseId));
          if (TestRailTestFilterRules.needRunTest(testCaseId)) {
            TestRailHelper.addTestResultForDisabledTest(
                testCaseId,
                CONFIG.userIdAssignedDisabledTests(),
                commentMessage.get());
          }
        });
  }

  private static String getTestCaseId(ITestNGMethod test) {
    return TestUtils.getTestAnnotation(test, TmsLink.class).value();
  }

  private static List<TicketSource> loadTicketSources() {
    return StreamSupport.stream(ServiceLoader.load(TicketSource.class).spliterator(), false)
        .filter(TicketSource::isEnabled)
        .toList();
  }

  private static Map<String, String> buildCommentsByTestCase(List<TicketSource> sources) {
    Map<String, List<String>> segmentsByTestCase = new java.util.HashMap<>();
    sources.forEach(source ->
        source.getTestCasesWithBugs().forEach((testCaseId, tickets) -> {
          if (tickets == null || tickets.isEmpty()) {
            return;
          }
          String ticketsString = tickets.stream()
              .map(source::buildTicketLink)
              .collect(Collectors.joining(StringConstant.COMMA_SPACE));
          String segment = "**%s:** %s".formatted(source.label(), ticketsString);
          segmentsByTestCase.computeIfAbsent(testCaseId, key -> new ArrayList<>()).add(segment);
        }));

    return segmentsByTestCase.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> "TC-%s is disabled via %s".formatted(
                entry.getKey(),
                String.join(StringConstant.SEMICOLON_SPACE, entry.getValue()))));
  }
}
