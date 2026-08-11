package dev.quokkify.testrail.utils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.constant.StringConstant;
import dev.quokkify.testrail.configs.TestRailConfiguration;
import dev.quokkify.testrail.models.CustomTag;
import dev.quokkify.testrail.models.StatusId;
import dev.quokkify.testrail.models.TestData;
import dev.quokkify.testrail.models.TestPlan;
import dev.quokkify.testrail.models.TestRailRunType;
import dev.quokkify.testrail.models.TestResult;
import dev.quokkify.testrail.models.TestRun;
import dev.quokkify.testrail.models.TestRunFromSuite;
import dev.quokkify.testrail.models.TestSuite;
import dev.quokkify.testrail.services.TestRailService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TestRailDataGenerator {

  /**
   * NOTE: can't use non private-package as it results in spotbugs errors.
   */
  static Map<Integer, List<TestData>> allTestsForExecute;
  static final Integer USER_ID;
  private static volatile TestRailDataGenerator instance;
  private static final TestRailService TEST_RAIL_SERVICE = TestRailService.getInstance();
  private static final Logger LOG = LogManager.getLogger(TestRailDataGenerator.class);
  private static final TestRailConfiguration CONFIG = ConfigRegistry.get(TestRailConfiguration.class);
  private static final boolean IS_TESTRAIL_ENABLED =
      !CONFIG.isTestrailDisabled() && StringUtils.isNotEmpty(CONFIG.testRailId());

  private TestRailDataGenerator() {
  }

  public static TestRailDataGenerator getInstance() {
    TestRailDataGenerator localInstance = instance;
    if (localInstance == null) {
      synchronized (TestRailDataGenerator.class) {
        localInstance = instance;
        if (localInstance == null) {
          instance = new TestRailDataGenerator();
        }
      }
    }
    return instance;
  }

  static {
    try {
      if (!IS_TESTRAIL_ENABLED || !NumberUtils.isCreatable(CONFIG.testRailId().trim())) {
        throw new RuntimeException("TestRail data initialization skipped.");
      }
      if (NumberUtils.isCreatable(CONFIG.testRailId())) {
        Integer testRailId = Integer.parseInt(CONFIG.testRailId());
        switch (getRunTypeById(testRailId)) {
          case TEST_SUITE -> {
            TestSuite testSuite = TestRailHelper.getTestSuite(testRailId);
            TestRunFromSuite newTestRun = TestRunFromSuite.builder().suiteId(testRailId)
                .name("%s:%s".formatted(testSuite.getName(), Instant.now().toString()))
                .includeAll(Boolean.TRUE).build();
            TEST_RAIL_SERVICE.setTestRun(TestRailHelper.createTestRunFromSuite(newTestRun));
            Integer currentRunId = TEST_RAIL_SERVICE.getTestRun().getId();
            allTestsForExecute = Map.of(currentRunId, TestRailHelper.getTests(currentRunId));
          }
          case TEST_PLAN -> {
            TEST_RAIL_SERVICE.setTestPlan(TestRailHelper.getTestPlan(testRailId));
            List<Integer> testRunIds = getTestPlanRunsIds(TEST_RAIL_SERVICE.getTestPlan());
            allTestsForExecute = new HashMap<>();
            testRunIds.forEach(id -> allTestsForExecute.put(id, TestRailHelper.getTests(id)));
          }
          case TEST_RUN -> {
            TEST_RAIL_SERVICE.setTestRun(TestRailHelper.getTestRun(testRailId));
            allTestsForExecute = Map.of(testRailId, TestRailHelper.getTests(testRailId));
          }
          default -> throw new RuntimeException(
              "Test run or test suite or test plan with id: %d does not exist".formatted(testRailId));
        }
      }
      USER_ID = TestRailHelper.getUserId();
      LOG.debug("{}",
          allTestsForExecute.entrySet().stream()
              .map(k ->
                  System.lineSeparator() + "Running test cases from TestRail (Run ID: %s). Test case IDs: %s"
                      .formatted(k.getKey(), k.getValue().stream()
                          .map(r -> r.getCaseId().toString())
                          .collect(Collectors.joining(StringConstant.COMMA_SPACE))))
              .collect(Collectors.joining(StringConstant.SEMICOLON_SPACE)));
      if (CONFIG.resetTestResultsToRetest()) {
        TestRailHelper.resetTestResultsToRetestForAllTestsForExecute();
      }
    } catch (Throwable e) {
      LOG.error("Error initializing TestRail data. Message and stack trace: {}", e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }

  static TestRailRunType getRunTypeById(int runId) {
    if (TestRailHelper.isExistTestSuite(runId)) {
      return TestRailRunType.TEST_SUITE;
    }
    if (TestRailHelper.isExistTestPlan(runId)) {
      return TestRailRunType.TEST_PLAN;
    }
    if (TestRailHelper.isExistTestRun(runId)) {
      return TestRailRunType.TEST_RUN;
    }
    return TestRailRunType.INVALID_TYPE;
  }

  public TestResult generatePassedTestResult() {
    return TestResult.builder().assignedToId(USER_ID).statusId(StatusId.PASSED.getId()).build();
  }

  public TestResult generateFailedTestResult(String commentMessage) {
    return generateFailedTestResult(USER_ID, commentMessage);
  }

  public TestResult generateFailedTestResult(Integer assignedUserId, String commentMessage) {
    return TestResult.builder()
        .assignedToId(assignedUserId)
        .statusId(StatusId.FAILED.getId())
        .comment(commentMessage)
        .build();
  }

  public TestResult generateSkippedTestResult(String commentMessage) {
    return TestResult.builder()
        .assignedToId(USER_ID)
        .statusId(StatusId.FAILED.getId())
        .comment(commentMessage)
        .build();
  }

  public TestResult generateRetestTestResult(String commentMessage) {
    return TestResult.builder()
        .assignedToId(USER_ID)
        .statusId(StatusId.RETEST.getId())
        .comment(commentMessage)
        .build();
  }

  /**
   * Transform tags from list of names to list of integers, e.g. 'Critical path;Limits' -> [37, 22]
   */
  public static List<Integer> collectCustomTagIds(List<String> customTags) {
    List<CustomTag> availableCustomTags = TestRailHelper.getAvailableCustomTags();
    return customTags.stream()
        .map(customTagFromPipeline -> getCustomTagId(availableCustomTags, customTagFromPipeline))
        .toList();
  }

  private static Integer getCustomTagId(List<CustomTag> availableCustomTags, String customTagFromPipeline) {
    return availableCustomTags.stream()
        .filter(availableCustomTag -> availableCustomTag.getName().equals(customTagFromPipeline))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Not found custom tag '%s'".formatted(customTagFromPipeline)))
        .getId();
  }

  private static List<Integer> getTestPlanRunsIds(TestPlan testPlan) {
    return testPlan.getEntries().stream()
        .flatMap(entry -> entry.getRuns().stream()
            .map(TestRun::getId))
        .toList();
  }
}
