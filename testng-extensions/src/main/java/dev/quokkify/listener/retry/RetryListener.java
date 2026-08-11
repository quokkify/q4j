package dev.quokkify.listener.retry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

/**
 * This class sets a custom retry analyzer for test methods to allow retrying failed tests.
 */
public class RetryListener implements IAnnotationTransformer {

  @Override
  public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
    annotation.setRetryAnalyzer(CustomRetryAnalyzer.class);
  }
}
