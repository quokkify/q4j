package dev.quokkify.listener.retry;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class CustomRetryAnalyzer implements IRetryAnalyzer {

  private final RetryChecker retryChecker = new RetryChecker();

  @Override
  public boolean retry(ITestResult testResult) {
    return retryChecker.isRetryNeeded(testResult);
  }
}
