package dev.quokkify.constant;

/**
 * Enumeration representing the scope of test execution in relation to known bugs.
 */
public enum BugExecutionScope {

  ALL_TESTS,
  /**
   * Only tests without known bugs are included in the execution scope.
   */
  EXCLUDE_TESTS_WITH_BUGS,
  /**
   * Only tests with known bugs are included in the execution scope.
   * This is useful for re-running tests to verify if bugs are resolved.
   */
  TESTS_WITH_BUGS
}
