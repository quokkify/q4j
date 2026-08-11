package dev.quokkify.testrail.listeners;

import java.util.Map;
import java.util.Objects;

import dev.quokkify.testrail.utils.TestRailHelper;
import dev.quokkify.util.TestUtils;

import io.qameta.allure.Allure;
import io.qameta.allure.TmsLink;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestResult;

/**
 * The TestRailCaseStructureToAllureListener class is an implementation of the IInvokedMethodListener
 * and ISuiteListener interfaces. It is designed to integrate TestRail test case structure into
 * Allure reports by adding relevant metadata, such as test case paths, to the Allure output.
 * This listener is controlled by an environment variable named "ENABLE_TMS_STRUCTURE_IN_ALLURE".
 * If the environment variable is not set or its value evaluates to true, the listener functionality
 * will be enabled. Otherwise, it will be disabled.
 * Functionality:
 * 1. On suite start, the listener fetches the breadcrumbs (path) of TestRail cases.
 * 2. After the invocation of a test method, it uses the TmsLink annotation to find the TestRail
 *    case ID associated with the test method. If the matching TestRail case is found, the test
 *    case's path is added as a label to the Allure report.
 */

public class TestRailCaseStructureToAllureListener implements IInvokedMethodListener, ISuiteListener {

  private static final Logger LOG = LogManager.getLogger(TestRailCaseStructureToAllureListener.class);
  private static final String CASE_PATH = "CASE_PATH";
  private Map<Integer, String> casesPath;
  private String enableTmsStructureInAllure = System.getenv("ENABLE_TMS_STRUCTURE_IN_ALLURE");

  @Override
  public boolean isEnabled() {
    return enableTmsStructureInAllure == null || Boolean.parseBoolean(enableTmsStructureInAllure);
  }

  @Override
  public void onStart(ISuite suite) {
    if (isEnabled()) {
      try {
        casesPath = TestRailHelper.getBreadCrumbs();
      } catch (Exception e) {
        LOG.warn("Can not fetch test case paths from TestRail. Listener is disabled", e);
        enableTmsStructureInAllure = Boolean.FALSE.toString();
      }
    }
  }

  @Override
  public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
    if (isEnabled()) {
      TmsLink testCaseIdAnnotation = TestUtils.getTestAnnotation(method.getTestMethod(), TmsLink.class);
      if (Objects.nonNull(testCaseIdAnnotation) && NumberUtils.isCreatable(testCaseIdAnnotation.value())) {
        Integer testCaseId = Integer.parseInt(testCaseIdAnnotation.value());
        try {
          String path = casesPath.get(TestRailHelper.getTestCase(testCaseId).getSectionId());
          if (Objects.isNull(path)) {
            path = StringUtils.EMPTY;
          }
          Allure.label(CASE_PATH, path);
        } catch (Exception e) {
          LOG.warn("Test case path not found for test case id: '%s'".formatted(testCaseId), e);
        }
      }
    }
  }
}
