package dev.quokkify.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.qameta.allure.TmsLink;
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

  /**
   * Reads repeatable {@link TmsLink} annotations of a method; falls back to the declaring class when the method has none.
   *
   * @param method the test method
   * @return non-blank trimmed distinct link values in declaration order, never {@code null}
   */
  public static List<String> getTmsLinks(Method method) {
    List<String> links = values(method.getAnnotationsByType(TmsLink.class));
    return links.isEmpty() ? values(method.getDeclaringClass().getAnnotationsByType(TmsLink.class)) : links;
  }

  /**
   * Reads repeatable {@link TmsLink} annotations of a TestNG method.
   *
   * @param method the TestNG test method
   * @return link values as in {@link #getTmsLinks(Method)}, or an empty list when the method has no reflection method
   */
  public static List<String> getTmsLinks(ITestNGMethod method) {
    Method reflectionMethod = method.getConstructorOrMethod().getMethod();
    return Objects.isNull(reflectionMethod) ? List.of() : getTmsLinks(reflectionMethod);
  }

  private static List<String> values(TmsLink[] annotations) {
    return Arrays.stream(annotations)
        .map(TmsLink::value)
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .distinct()
        .toList();
  }
}
