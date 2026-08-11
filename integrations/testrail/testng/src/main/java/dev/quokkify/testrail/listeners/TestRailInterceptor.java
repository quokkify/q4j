package dev.quokkify.testrail.listeners;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.constant.StringConstant;
import dev.quokkify.testrail.configs.TestRailConfiguration;
import dev.quokkify.testrail.utils.TestRailTestFilterRules;
import dev.quokkify.util.TestUtils;

import io.qameta.allure.TmsLink;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

/**
 * Interceptor filters tests to run. Skip tests without {@link TmsLink}
 * Run only test filtered by rules from {@link TestRailTestFilterRules}
 */
public class TestRailInterceptor implements IMethodInterceptor {

  private static final Logger LOG = LogManager.getLogger(TestRailInterceptor.class);
  private static final TestRailConfiguration CONFIG = ConfigRegistry.get(TestRailConfiguration.class);
  private static final boolean IS_TESTRAIL_ENABLED =
      !CONFIG.isTestrailDisabled() && StringUtils.isNotEmpty(CONFIG.testRailId());

  @Override
  public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
    if (!IS_TESTRAIL_ENABLED) return methods;
    List<IMethodInstance> filteredMethods = methods.stream()
        .filter(methodInstance ->
            needRunTest(TestUtils.getTestAnnotation(methodInstance.getMethod(), TmsLink.class)))
        .toList();
    logFilteredTests(filteredMethods);
    return filteredMethods;
  }

  private boolean needRunTest(TmsLink testCaseIdAnnotation) {
    if (Objects.isNull(testCaseIdAnnotation)) {
      LOG.error("TmsLink annotation is not found, test has not been run");
      return false;
    } else if (Objects.isNull(testCaseIdAnnotation.value())) {
      LOG.error("TmsLink annotation does not contain a value, test has not been run");
      return false;
    } else {
      return TestRailTestFilterRules.needRunTest(testCaseIdAnnotation.value());
    }
  }

  private void logFilteredTests(List<IMethodInstance> filteredMethods) {
    String interceptMethods = filteredMethods.stream()
        .filter(methodInstances ->
            Objects.nonNull(TestUtils.getTestAnnotation(methodInstances.getMethod(), TmsLink.class)))
        .map(method -> TestUtils.getTestAnnotation(method.getMethod(), TmsLink.class).value())
        .collect(Collectors.joining(StringConstant.COMMA_SPACE)).trim();
    if (!interceptMethods.isEmpty()) {
      LOG.debug("Test cases to run: {}", interceptMethods);
    }
  }
}
