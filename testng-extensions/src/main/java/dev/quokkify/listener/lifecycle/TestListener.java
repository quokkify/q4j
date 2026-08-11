package dev.quokkify.listener.lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.quokkify.constant.StringConstant;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * This listener logs test statuses and pretty prints stack trace for failed tests.
 */
public class TestListener implements ITestListener {

  private static final Logger LOG = LogManager.getLogger();

  @Override
  public void onTestStart(ITestResult result) {
    logMessage(Level.INFO, "STARTED", result.getTestClass().getName(), result.getMethod().getMethodName());
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    logMessage(Level.INFO, "PASSED", result.getTestClass().getName(), result.getMethod().getMethodName());
  }

  @Override
  public void onTestFailure(ITestResult result) {
    logMessage(Level.WARN, "FAILED", result.getTestClass().getName(), result.getMethod().getMethodName());
    LOG.warn("Additional info:\n{}", filterPlatformStackTrace(result.getThrowable()));
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    logMessage(Level.WARN, "SKIPPED", result.getTestClass().getName(), result.getMethod().getMethodName());
  }

  private void logMessage(Level logLevel, String testStatus, String className, String methodName) {
    LOG.log(logLevel, "\n____________[ TEST {}.{} {} ]____________", shortClassName(className), methodName, testStatus);
  }

  private String filterPlatformStackTrace(Throwable throwable) {
    String errorMessage =
        Arrays.stream(ExceptionUtils.getRootCauseStackTrace(throwable)).findFirst().orElse("Throwable was not found");
    String platformStackTrace = Arrays.stream(ExceptionUtils.getRootCauseStackTrace(throwable))
        .filter(el -> el.contains("dev.quokkify"))
        .collect(Collectors.joining(StringUtils.LF));
    return errorMessage.concat(StringUtils.LF).concat(platformStackTrace);
  }

  private String shortClassName(String className) {
    ArrayList<String> testPath = new ArrayList<>(Arrays.stream(className.split("\\.")).toList());
    int testsPackageIndex = testPath.indexOf("tests");
    if (testsPackageIndex > 0) {
      IntStream.range(0, testsPackageIndex).forEach(index -> testPath.set(index, testPath.get(index).substring(0, 1)));
    }
    return String.join(StringConstant.DOT, testPath);
  }
}
