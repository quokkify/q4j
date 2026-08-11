package dev.quokkify.testrail.constants;

/**
 * Rule that is used to filter tests to run.
 */
@FunctionalInterface
public interface TestFilterRole {

  /**
   * Returns {@code true} if the test needs to be run. Otherwise {@code false}
   *
   * @param testCaseId case id from TestRail
   */
  Boolean filterByCaseId(String testCaseId);
}
