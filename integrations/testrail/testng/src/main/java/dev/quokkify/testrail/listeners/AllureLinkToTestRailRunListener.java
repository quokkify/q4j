package dev.quokkify.testrail.listeners;

import java.util.Objects;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.testrail.configs.TestRailConfiguration;
import dev.quokkify.testrail.models.TestData;
import dev.quokkify.testrail.utils.TestRailHelper;
import dev.quokkify.util.TestUtils;

import io.qameta.allure.Allure;
import io.qameta.allure.TmsLink;
import io.qameta.allure.model.Link;
import io.qameta.allure.util.ResultsUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Listener responsible for updating links in the Allure Report.
 * This listener adds dynamic links to test cases in Test Runs.
 *
 * <p>
 * The links relate to test management systems, such as TestRail or Jira, and
 * are dynamically generated for each Test Run.
 * </p>
 *
 * <ul>
 *   <li><b>Original Case:</b> C12345</li>
 *   <li><b>Dynamic Case:</b> Txxxxx (from a dynamic Test Run)</li>
 * </ul>
 *
 * <b>@TmsLink("12345")</b>
 * This annotation links the listener to the corresponding test case in the test management system.
 */
public class AllureLinkToTestRailRunListener implements ITestListener {

  private static final TestRailConfiguration CONFIG = ConfigRegistry.get(TestRailConfiguration.class);
  private static final boolean IS_TESTRAIL_ENABLED =
      !CONFIG.isTestrailDisabled() && StringUtils.isNotEmpty(CONFIG.testRailId());

  @Override
  public void onTestStart(ITestResult testResult) {
    updateTestCaseTmsLinks(testResult);
    if (IS_TESTRAIL_ENABLED && NumberUtils.isCreatable(CONFIG.testRailId())) {
      String testId = String.valueOf(getTestData(testResult).getId());
      Link link = ResultsUtils.createTmsLink(testId);
      Allure.link("T%s".formatted(testId), link.getUrl().replace("cases", "tests"));
    }
  }

  private static TestData getTestData(ITestResult testResult) {
    int originalTestCaseId = Integer.parseInt(getTestCaseId(testResult));
    int currentTestRunId = TestRailHelper.getTestRunId(originalTestCaseId);
    return TestRailHelper.getActualTest(currentTestRunId, originalTestCaseId);
  }

  private static String getTestCaseId(ITestResult testResult) {
    TmsLink testCaseIdAnnotation = TestUtils.getTestAnnotation(testResult.getMethod(), TmsLink.class);
    if (Objects.nonNull(testCaseIdAnnotation) && Objects.nonNull(testCaseIdAnnotation.value())) {
      return TestUtils.getTestAnnotation(testResult.getMethod(), TmsLink.class).value();
    } else {
      throw new RuntimeException(
          "Test has no test case id (TmsLink), test name: %s".formatted(testResult.getMethod().getMethodName()));
    }
  }

  private static void updateTestCaseTmsLinks(ITestResult testResult) {
    Allure.getLifecycle().updateTestCase(allureTestResult ->
        allureTestResult.getLinks()
            .stream()
            .filter(link -> link.getName().equals(getTestCaseId(testResult)))
            .findFirst()
            .ifPresent(link -> link.setName("C%s".formatted(getTestCaseId(testResult)))));
  }
}
