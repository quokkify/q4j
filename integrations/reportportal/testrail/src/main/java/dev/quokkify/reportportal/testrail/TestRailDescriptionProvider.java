package dev.quokkify.reportportal.testrail;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.reportportal.spi.TmsDescriptionProvider;
import dev.quokkify.testrail.configs.TestRailConfiguration;
import dev.quokkify.testrail.services.TestRailService;

import org.apache.commons.lang3.StringUtils;

public class TestRailDescriptionProvider implements TmsDescriptionProvider {

  private static final TestRailConfiguration CONFIG = ConfigRegistry.get(TestRailConfiguration.class);

  @Override
  public boolean isEnabled() {
    return !CONFIG.isTestrailDisabled() && StringUtils.isNotEmpty(CONFIG.testRailId());
  }

  @Override
  public String testCaseUrl(String tmsId) {
    return StringUtils.isNotBlank(CONFIG.testRailCaseUrl())
        ? CONFIG.testRailCaseUrl().formatted(tmsId)
        : StringUtils.EMPTY;
  }

  @Override
  public String enrichLaunchDescription() {
    TestRailService testRailService = TestRailService.getInstance();
    if (testRailService.isTestPlan() && StringUtils.isNotBlank(CONFIG.testRailTestPlanUrl())) {
      String planId = testRailService.getTestPlan().getId().toString();
      return "**TestRail plan id:** [%s](%s)%n"
          .formatted(planId, CONFIG.testRailTestPlanUrl().formatted(planId));
    } else if (testRailService.isTestRun() && StringUtils.isNotBlank(CONFIG.testRailTestRunUrl())) {
      String runId = testRailService.getTestRun().getId().toString();
      return "**TestRail run id:** [%s](%s)%n"
          .formatted(runId, CONFIG.testRailTestRunUrl().formatted(runId));
    }
    return StringUtils.EMPTY;
  }
}
