package dev.quokkify.util;

import java.lang.annotation.Annotation;

import org.testng.ITestNGMethod;

/**
 * Utility class for working with TestNG test annotations.
 */
public class TestUtils {

  private TestUtils() {
  }

  /**
   * Retrieves a specific annotation from a TestNG test method.
   *
   * @param test           the TestNG test method
   * @param annotationType the type of the annotation to retrieve
   * @param <T>            the type of the annotation
   * @return the annotation if found, or {@code null} if the annotation is not present on the method
   */
  public static <T extends Annotation> T getTestAnnotation(ITestNGMethod test, Class<T> annotationType) {
    return test.getConstructorOrMethod().getMethod().getAnnotation(annotationType);
  }
}
