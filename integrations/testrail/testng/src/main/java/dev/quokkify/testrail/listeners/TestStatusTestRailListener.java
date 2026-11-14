package dev.quokkify.testrail.listeners;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.testrail.configs.TestRailConfiguration;
import dev.quokkify.testrail.utils.TestRailHelper;
import dev.quokkify.tms.listeners.TmsListener;
import dev.quokkify.util.TestUtils;

import io.qameta.allure.TmsLink;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Listener updates test statuses in TestRail and adds error message if test is not passed.
 *
 * @deprecated use {@code dev.quokkify.tms.listeners.TmsListener}
 */
@Deprecated(forRemoval = true)
public class TestStatusTestRailListener implements ITestListener {

  private static final Logger LOG = LogManager.getLogger(TestStatusTestRailListener.class);
  private static final AtomicBoolean WARNED = new AtomicBoolean();
  private static final TestRailConfiguration CONFIG = ConfigRegistry.get(TestRailConfiguration.class);
  private static final boolean IS_TESTRAIL_ENABLED =
      !CONFIG.isTestrailDisabled() && StringUtils.isNotEmpty(CONFIG.testRailId());

  @Override
  public void onTestSuccess(ITestResult result) {
    if (isSupersededByTmsListener() || !IS_TESTRAIL_ENABLED) return;
    TestRailHelper.addTestResultForPassedTest(getTestCaseId(result));
  }

  @Override
  public void onTestFailure(ITestResult result) {
    if (isSupersededByTmsListener() || !IS_TESTRAIL_ENABLED) return;
    TestRailHelper.addTestResultForFailedTest(getTestCaseId(result), getErrorMessage(result));
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    if (isSupersededByTmsListener() || !IS_TESTRAIL_ENABLED) return;
    TestRailHelper.addTestResultForSkippedTest(getTestCaseId(result), getErrorMessage(result));
  }

  private static boolean isSupersededByTmsListener() {
    if (!TmsListener.isActive()) {
      return false;
    }
    if (WARNED.compareAndSet(false, true)) {
      LOG.warn("TestStatusTestRailListener is deprecated and does nothing while TmsListener is registered, remove it");
    }
    return true;
  }

  private String getErrorMessage(ITestResult result) {
    Throwable throwable = result.getThrowable();
    return Objects.nonNull(throwable) ? throwable.getMessage() : "Error message not found";
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
}
