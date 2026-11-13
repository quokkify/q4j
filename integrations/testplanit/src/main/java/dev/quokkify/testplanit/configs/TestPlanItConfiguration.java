package dev.quokkify.testplanit.configs;

import org.aeonbits.owner.Config;

/**
 * Configuration of the TestPlanIt integration.
 *
 * <ul>
 *     <li><b>isTestPlanItDisabled</b> - Flag to disable the integration.</li>
 *     <li><b>apiUrl</b> - Base URL of TestPlanIt, for example {@code https://testplanit.example.com}.</li>
 *     <li><b>apiToken</b> - API token ({@code tpi_...}). Pass it through the environment, never commit it.</li>
 *     <li><b>projectId</b> - Id of the project that holds the cases.</li>
 *     <li><b>runId</b> - Id of an existing run to append results to. A new run is created when empty.</li>
 *     <li><b>runName</b> - Name of the new run. Defaults to {@code q4j <timestamp>}.</li>
 *     <li><b>configId</b> - Id of the configuration assigned to the new run.</li>
 *     <li><b>milestoneId</b> - Id of the milestone assigned to the new run.</li>
 *     <li><b>completeRun</b> - Flag to mark the run completed after all suites finish.</li>
 * </ul>
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:local_resources/testplanit-config.properties",
    "classpath:testplanit-config.properties"
})
public interface TestPlanItConfiguration extends Config {

  @Key("IS_TESTPLANIT_DISABLED")
  @DefaultValue("false")
  Boolean isTestPlanItDisabled();

  @Key("TESTPLANIT_API_URL")
  String apiUrl();

  @Key("TESTPLANIT_API_TOKEN")
  String apiToken();

  @Key("TESTPLANIT_PROJECT_ID")
  Integer projectId();

  @Key("TESTPLANIT_RUN_ID")
  Integer runId();

  @Key("TESTPLANIT_RUN_NAME")
  String runName();

  @Key("TESTPLANIT_CONFIG_ID")
  Integer configId();

  @Key("TESTPLANIT_MILESTONE_ID")
  Integer milestoneId();

  @Key("TESTPLANIT_COMPLETE_RUN")
  @DefaultValue("false")
  Boolean completeRun();
}
