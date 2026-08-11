package dev.quokkify.listener.retry;

import org.testng.IConfigurable;
import org.testng.IConfigureCallBack;
import org.testng.ITestResult;

public class CustomConfigRetryListener implements IConfigurable {

  @Override
  public void run(IConfigureCallBack callBack, ITestResult testResult) {
    RetryChecker retryChecker = new RetryChecker();
    while (retryChecker.runAndCheckIsRetryNeeded(callBack, testResult)) {
    }
  }
}
