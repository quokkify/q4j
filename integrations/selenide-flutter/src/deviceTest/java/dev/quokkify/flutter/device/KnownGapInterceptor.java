package dev.quokkify.flutter.device;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger LOG = LoggerFactory.getLogger(KnownGapInterceptor.class);

  @Override
  public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
    if (!Boolean.getBoolean(PROPERTY)) {
      return methods;
    }
    return methods.stream().filter(method -> !excluded(method)).toList();
  }

  private static boolean excluded(IMethodInstance method) {
    if (!isKnownGap(method)) {
      return false;
    }
    LOG.info("Excluding known gap {}.{} on {} (groups {})", method.getMethod().getRealClass().getSimpleName(),
        method.getMethod().getMethodName(), method.getInstance(), Arrays.toString(method.getMethod().getGroups()));
    return true;
  }

  private static boolean isKnownGap(IMethodInstance method) {
    if (!(method.getInstance() instanceof BaseMobileTest test)) {
      return false;
    }
    String group = GROUP_PREFIX + test.mode.label();
    return Arrays.asList(method.getMethod().getGroups()).contains(group);
  }
}
