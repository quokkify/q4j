package dev.quokkify.listener.retry;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.config.TestNGExtension;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IConfigureCallBack;
import org.testng.ITestContext;
import org.testng.ITestResult;

public class RetryChecker {

  private static final Logger LOG = LogManager.getLogger(RetryChecker.class);
  private static final TestNGExtension CONFIG = ConfigRegistry.get(TestNGExtension.class);

  private int tryCount = 0;

  /**
   * Check if the test needs to be restarted and logs the error if needed.
   *
   * @param testResult test result
   * @return true if the error is expected and the number of restarts is not greater than expected. Otherwise false
   */
  public boolean isRetryNeeded(ITestResult testResult) {
    boolean isRetryNeeded = !testResult.isSuccess()
        && isExceptionMatched(testResult)
        && isRetryNeededAccordingRetryCount();
    if (isRetryNeeded) {
      logError(testResult);
    }
    return isRetryNeeded;
  }

  /**
   * Run configuration method (e.g. methods with annotations BeforeSuite, BeforeClass, BeforeMethod, AfterClass...).
   * And check if the test needs to be restarted and logs the error if needed
   *
   * @param testResult test result
   * @return true if the error is expected and the number of restarts is not greater than expected. Otherwise false
   */
  public boolean runAndCheckIsRetryNeeded(IConfigureCallBack callBack, ITestResult testResult) {
    callBack.runConfigurationMethod(testResult);
    return isRetryNeeded(testResult);
  }

  private void logError(ITestResult testResult) {
    String throwable = testResult.getThrowable() == null
        ? "No throwable"
        : "Error message: %s%nStack trace:%n%s".formatted(
        ExceptionUtils.getRootCauseMessage(testResult.getThrowable()),
        ExceptionUtils.getStackTrace(testResult.getThrowable()));
    LOG.error("""
            Retry method '{}' with parameters:%n\
            {}%n\
            Сaught throwable:%n{}%n\
            """,
        testResult.getMethod().getMethodName(), StringUtils.join(testResult.getParameters(), "%n"), throwable);
  }

  private static boolean isExceptionMatched(ITestResult testResult) {
    Throwable throwable = testResult.getThrowable();
    ITestContext testContext = testResult.getTestContext();
    List<Class<? extends Throwable>> exceptionsToRetry = getExceptionsToRetry(testContext);
    if (throwable instanceof InvocationTargetException) {
      throwable = throwable.getCause();
    }
    final Throwable finalThrowable = throwable;
    return exceptionsToRetry.stream()
        .anyMatch(exceptionClass -> exceptionClass.isInstance(finalThrowable));
  }

  private static List<Class<? extends Throwable>> getExceptionsToRetry(ITestContext context) {
    Object attribute = context.getAttribute("exceptionsToRetry");
    return (attribute instanceof List)
        ? (List<Class<? extends Throwable>>) attribute
        : Collections.emptyList();
  }

  private boolean isRetryNeededAccordingRetryCount() {
    if (tryCount < CONFIG.retryCount()) {
      tryCount++;
      return true;
    } else {
      return false;
    }
  }
}
