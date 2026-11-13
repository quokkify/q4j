package dev.quokkify.testrail.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.testrail.configs.TestRailConfiguration;
import dev.quokkify.testrail.utils.TestRailHelper;
import dev.quokkify.testrail.utils.TestRailTestFilterRules;
import dev.quokkify.tms.listeners.TmsListener;
import dev.quokkify.tms.tickets.TicketComments;
import dev.quokkify.tms.tickets.TicketSource;
import dev.quokkify.util.TestUtils;

import io.qameta.allure.TmsLink;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestNGMethod;

/**
 * Listener adds info (description, issue link) about disabled tests to TestRail.
 * Add only to tests filtered by rules from {@link TestRailTestFilterRules}
 *
 * @deprecated use {@code dev.quokkify.tms.listeners.TmsListener}
 */
@Deprecated(forRemoval = true)
public class AddDisabledTestsToTestRailListener implements ISuiteListener {

  private static final Logger LOG = LogManager.getLogger(AddDisabledTestsToTestRailListener.class);
  private static final AtomicBoolean WARNED = new AtomicBoolean();
  private static final TestRailConfiguration CONFIG = ConfigRegistry.get(TestRailConfiguration.class);
  private static final boolean IS_TESTRAIL_ENABLED =
      !CONFIG.isTestrailDisabled() && StringUtils.isNotEmpty(CONFIG.testRailId());

  @Override
  public void onFinish(ISuite suite) {
    if (isSupersededByTmsListener() || !IS_TESTRAIL_ENABLED) return;
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
    List<TicketSource> sources = TicketComments.load();
    if (sources.isEmpty()) {
      return;
    }
    Map<String, String> commentsByTestCase = TicketComments.byCaseId(sources);
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

  private static boolean isSupersededByTmsListener() {
    if (!TmsListener.isActive()) {
      return false;
    }
    if (WARNED.compareAndSet(false, true)) {
      LOG.warn("AddDisabledTestsToTestRailListener is deprecated and does nothing while TmsListener is registered, remove it");
    }
    return true;
  }
}
