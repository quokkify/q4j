package dev.quokkify.flutter.device;

import java.util.Arrays;
import java.util.List;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

/**
 * Leaves out the tests in the {@code known-gap-<mode>} group of their own instance's mode when the system property
 * {@code mobile.excludeKnownGaps} is {@code true}, so a mode's suite runs only what that mode can do.
 */
public class KnownGapInterceptor implements IMethodInterceptor {

  static final String PROPERTY = "mobile.excludeKnownGaps";
  static final String GROUP_PREFIX = "known-gap-";

  @Override
  public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
    if (!Boolean.getBoolean(PROPERTY)) {
      return methods;
    }
    return methods.stream().filter(method -> !isKnownGap(method)).toList();
  }

  private static boolean isKnownGap(IMethodInstance method) {
    if (!(method.getInstance() instanceof BaseMobileTest test)) {
      return false;
    }
    String group = GROUP_PREFIX + test.mode.label();
    return Arrays.asList(method.getMethod().getGroups()).contains(group);
  }
}
