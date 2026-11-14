package dev.quokkify.listener.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dev.quokkify.util.SplitUtils;
import dev.quokkify.util.TestUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

/**
 * Interceptor filters tests to run. Skip tests that are not in the env variable {@value #TEST_CASE_IDS_TO_RUN_ENV_NAME}
 */
public class CustomCasesInterceptor implements IMethodInterceptor {

  private static final Logger LOG = LogManager.getLogger();
  private static final String TEST_CASE_IDS_TO_RUN_ENV_NAME = "TEST_CASE_IDS";
  private static final List<String> TEST_CASE_IDS = SplitUtils.splitEnvVariable(TEST_CASE_IDS_TO_RUN_ENV_NAME);

  @Override
  public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
    if (TEST_CASE_IDS.isEmpty()) return methods;
    List<String> filteredTestCases = new ArrayList<>();
    List<IMethodInstance> filteredMethods = methods.stream()
        .filter(methodInstance -> {
          List<String> caseIds = TestUtils.getTmsLinks(methodInstance.getMethod());
          boolean isTestRequired = isTestRequired(caseIds, TEST_CASE_IDS);
          if (isTestRequired) {
            filteredTestCases.addAll(caseIds);
          }
          return isTestRequired;
        })
        .collect(Collectors.toList());
    LOG.info("After filtering, the following list of cases was formed: {}",
        String.join(", ", filteredTestCases));
    return filteredMethods;
  }

  static boolean isTestRequired(List<String> caseIds, List<String> requiredIds) {
    return caseIds.stream().anyMatch(requiredIds::contains);
  }
}
