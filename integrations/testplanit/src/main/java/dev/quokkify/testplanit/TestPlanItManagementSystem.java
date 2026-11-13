package dev.quokkify.testplanit;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.testplanit.configs.TestPlanItConfiguration;
import dev.quokkify.testplanit.models.RunSettings;
import dev.quokkify.testplanit.services.TestPlanItFeignClient;
import dev.quokkify.testplanit.services.TestPlanItService;
import dev.quokkify.tms.TestManagementSystem;
import dev.quokkify.tms.TmsResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * TestPlanIt as a {@link TestManagementSystem}. Active when it is the only TMS provider on the classpath;
 * case ids come from {@code @TmsLink} and must be numeric.
 */
public class TestPlanItManagementSystem implements TestManagementSystem {

  public static final String ID = "testplanit";

  private static final Logger LOG = LogManager.getLogger(TestPlanItManagementSystem.class);

  private final TestPlanItConfiguration config;
  private TestPlanItService service;

  public TestPlanItManagementSystem() {
    this(ConfigRegistry.get(TestPlanItConfiguration.class));
  }

  TestPlanItManagementSystem(TestPlanItConfiguration config) {
    this.config = config;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public boolean isConfigured() {
    return !Boolean.TRUE.equals(config.isTestPlanItDisabled())
        && StringUtils.isNoneBlank(config.apiUrl(), config.apiToken())
        && Objects.nonNull(config.projectId());
  }

  @Override
  public void startRun(Set<String> caseIds) {
    service().startRun(caseIds);
    LOG.info("TestPlanIt run: {}", runUrl());
  }

  @Override
  public void report(TmsResult result) {
    service().report(result);
  }

  @Override
  public void finishRun() {
    service().finishRun();
  }

  /**
   * @return URL of the run in the TestPlanIt UI, or an empty string before the run is opened
   */
  public String runUrl() {
    return service().getRun()
        .map(run -> "%s/projects/runs/%d/%d".formatted(
            StringUtils.removeEnd(config.apiUrl().trim(), "/"), run.projectId(), run.id()))
        .orElse(StringUtils.EMPTY);
  }

  private synchronized TestPlanItService service() {
    if (Objects.isNull(service)) {
      service = new TestPlanItService(
          new TestPlanItFeignClient(config.apiUrl(), config.apiToken()),
          new RunSettings(
              config.projectId(),
              config.runId(),
              StringUtils.defaultIfBlank(config.runName(), "q4j " + Instant.now()),
              config.configId(),
              config.milestoneId(),
              Boolean.TRUE.equals(config.completeRun())));
    }
    return service;
  }
}
