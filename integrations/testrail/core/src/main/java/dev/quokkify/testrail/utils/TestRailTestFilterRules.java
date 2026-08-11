package dev.quokkify.testrail.utils;

import java.util.ArrayList;
import java.util.List;

import dev.quokkify.testrail.constants.TestFilterRole;
import dev.quokkify.testrail.models.StatusId;
import dev.quokkify.testrail.models.TestData;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class contains rules that are used to filter tests to run. You can add custom rules for yourself in beforeSuite or
 * other places
 */
public final class TestRailTestFilterRules {

  private static final Logger LOG = LogManager.getLogger(TestRailTestFilterRules.class);
  private static final List<TestFilterRole> RULES = getDefaultTestFilterRules();

  private TestRailTestFilterRules() {
  }

  public static void addTestFilterRule(TestFilterRole testFilter) {
    RULES.add(testFilter);
  }

  public static boolean needRunTest(String testCaseId) {
    return RULES.stream().allMatch(testFilterRole -> testFilterRole.filterByCaseId(testCaseId));
  }

  private static List<TestFilterRole> getDefaultTestFilterRules() {
    ArrayList<TestFilterRole> testFilters = new ArrayList<>();
    testFilters.add(TestRailTestFilterRules::isTestInRun);
    testFilters.add(TestRailTestFilterRules::isAssignedToAutoUserOrUnassigned);
    testFilters.add(TestRailTestFilterRules::isTestNotPassed);
    return testFilters;
  }

  private static boolean isTestNotPassed(String testCaseId) {
    return getTestData(testCaseId).getStatusId() != StatusId.PASSED.getId();
  }

  private static boolean isTestInRun(String testCaseId) {
    return TestRailDataGenerator.allTestsForExecute.values().stream().flatMap(List::stream)
        .anyMatch(testData -> testData.getCaseId().equals(Integer.parseInt(testCaseId)));
  }

  /**
   * Check that case is assigned to AUTO user or unassigned.
   */
  private static boolean isAssignedToAutoUserOrUnassigned(String testCaseId) {
    boolean isAssignedToAutoUserOrUnassigned =
        TestRailDataGenerator.allTestsForExecute.values().stream().flatMap(List::stream)
            .filter(test -> test.getCaseId().equals(Integer.parseInt(testCaseId)))
            .anyMatch(test -> test.getAssignedToId() == null
                || test.getAssignedToId().equals(TestRailDataGenerator.USER_ID));
    LOG.debug("Test '{}' is {}assigned to AUTO/Unassigned", testCaseId,
        isAssignedToAutoUserOrUnassigned ? StringUtils.EMPTY : "not ");
    return isAssignedToAutoUserOrUnassigned;
  }

  private static TestData getTestData(String testCaseId) {
    return TestRailDataGenerator.allTestsForExecute.values().stream().flatMap(List::stream)
        .filter(test -> test.getCaseId().equals(Integer.parseInt(testCaseId)))
        .findFirst()
        .orElseThrow(
            () -> new RuntimeException("Test case with id '%s' not found in test run".formatted(testCaseId)));
  }
}
