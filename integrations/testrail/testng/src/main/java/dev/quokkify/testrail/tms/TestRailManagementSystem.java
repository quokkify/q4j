package dev.quokkify.testrail.tms;

import java.util.function.Predicate;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.testrail.configs.TestRailConfiguration;
import dev.quokkify.testrail.utils.TestRailHelper;
import dev.quokkify.testrail.utils.TestRailTestFilterRules;
import dev.quokkify.tms.TestManagementSystem;
import dev.quokkify.tms.TmsResult;

import org.apache.commons.lang3.StringUtils;

/**
 * TestRail as a {@link TestManagementSystem} for {@code TmsListener}. Delegates to {@link TestRailHelper},
 * so results go to the same runs as with the TestRail listeners. Cases are linked with {@code @TmsLink}.
 */
public class TestRailManagementSystem implements TestManagementSystem {

  public static final String ID = "testrail";

  private final TestRailConfiguration config;
  private final Predicate<String> runFilter;

  public TestRailManagementSystem() {
    this(ConfigRegistry.get(TestRailConfiguration.class), TestRailTestFilterRules::needRunTest);
  }

  TestRailManagementSystem(TestRailConfiguration config) {
    this(config, TestRailTestFilterRules::needRunTest);
  }

  TestRailManagementSystem(TestRailConfiguration config, Predicate<String> runFilter) {
    this.config = config;
    this.runFilter = runFilter;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public boolean isConfigured() {
    return !Boolean.TRUE.equals(config.isTestrailDisabled()) && StringUtils.isNotEmpty(config.testRailId());
  }

  @Override
  public boolean shouldRun(String caseId) {
    return runFilter.test(caseId);
  }

  @Override
  public void report(TmsResult result) {
    switch (result.status()) {
      case PASSED -> TestRailHelper.addTestResultForPassedTest(result.caseId());
      case FAILED -> TestRailHelper.addTestResultForFailedTest(result.caseId(), result.comment());
      case SKIPPED -> TestRailHelper.addTestResultForSkippedTest(result.caseId(), result.comment());
      case BLOCKED -> {
        if (shouldRun(result.caseId())) {
          TestRailHelper.addTestResultForDisabledTest(
              result.caseId(), config.userIdAssignedDisabledTests(), result.comment());
        }
      }
      default -> throw new IllegalArgumentException("Unsupported status: " + result.status());
    }
  }

  @Override
  public void finishRun() {
    if (Boolean.TRUE.equals(config.closeTestRun())) {
      TestRailHelper.closeActualTestRuns();
    }
    if (Boolean.TRUE.equals(config.deleteTestRun())) {
      TestRailHelper.deleteActualTestRuns();
    }
  }
}
