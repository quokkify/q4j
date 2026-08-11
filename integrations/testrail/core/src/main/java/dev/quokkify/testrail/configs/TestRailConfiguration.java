package dev.quokkify.testrail.configs;

import org.aeonbits.owner.Config;

/**
 * Class that represents the configuration file for TestRail.
 * The following fields are available:
 *
 * <ul>
 *     <li><b>isTestrailDisabled</b> - Flag to disable TestRail integration.</li>
 *     <li><b>baseUrl</b> - The base URL of TestRail.</li>
 *     <li><b>user</b> - The username in TestRail.</li>
 *     <li><b>password</b> - The password for the user.</li>
 *     <li><b>projectId</b> - The ID of your project in TestRail.</li>
 *     <li><b>baseSuiteId</b> - The ID of the suite containing all test cases.</li>
 *     <li><b>testRailId</b> - The ID of the suite, run, or plan where test results will be saved.</li>
 *     <li><b>userIdAssignedDisabledTests</b> - The user ID to which disabled tests will be assigned.</li>
 *     <li><b>closeTestRun</b> - Flag to automatically close the test run after completion.</li>
 *     <li><b>deleteTestRun</b> - Flag to delete the test run after completion.</li>
 *     <li><b>resetTestResultsToRetest</b> - Flag to reset test results to the "Retest" status.</li>
 *     <li><b>testRailCaseUrl</b> - Template URL for accessing a specific test case in TestRail.</li>
 *     <li><b>testRailTestPlanUrl</b> - Template URL for accessing a test plan in TestRail.</li>
 *     <li><b>testRailTestRunUrl</b> - Template URL for accessing a test run in TestRail.</li>
 * </ul>
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:local_resources/testrail-config.properties",
    "classpath:testrail-config.properties"
})
public interface TestRailConfiguration extends Config {

  @Key("IS_TESTRAIL_DISABLED")
  @DefaultValue("false")
  Boolean isTestrailDisabled();

  @Key("TESTRAIL_URL")
  String baseUrl();

  @Key("TESTRAIL_USER")
  String user();

  @Key("TESTRAIL_PASSWORD")
  String password();

  @Key("TESTRAIL_PROJECT_ID")
  Integer projectId();

  @Key("TESTRAIL_BASE_SUITE_ID")
  Integer baseSuiteId();

  @Key("TESTRAIL_USER_ID_ASSIGNED_DISABLED_TESTS")
  Integer userIdAssignedDisabledTests();

  @Key("TESTRAIL_ID")
  String testRailId();

  @Key("CLOSE_TEST_RUN")
  @DefaultValue("false")
  Boolean closeTestRun();

  @Key("DELETE_TEST_RUN")
  @DefaultValue("false")
  Boolean deleteTestRun();

  @Key("TESTRAIL_RESET_TEST_RESULTS_TO_RETEST")
  @DefaultValue("false")
  Boolean resetTestResultsToRetest();

  @Key("TESTRAIL_CASE_URL")
  String testRailCaseUrl();

  @Key("TESTRAIL_TESTPLAN_URL")
  String testRailTestPlanUrl();

  @Key("TESTRAIL_TESTRUN_URL")
  String testRailTestRunUrl();
}
