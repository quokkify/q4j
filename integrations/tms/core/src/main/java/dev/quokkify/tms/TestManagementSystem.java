package dev.quokkify.tms;

import java.util.Set;

/**
 * Test management system that receives automated test results.
 * Implementations are discovered with {@link java.util.ServiceLoader}.
 */
public interface TestManagementSystem {

  /**
   * @return id of the system used in logs, for example {@code testplanit}
   */
  String id();

  /**
   * @return {@code true} if all settings required to report results are present
   */
  boolean isConfigured();

  /**
   * Decides whether a test linked to the case should be executed.
   * A test linked to several cases runs if any of them should run.
   *
   * @param caseId case id from {@code @TmsLink}
   * @return {@code true} if the test should run
   */
  default boolean shouldRun(String caseId) {
    return true;
  }

  /**
   * Prepares the run before tests start. Can be called more than once, for example once per suite.
   *
   * @param caseIds ids of the cases that are going to be executed
   */
  default void startRun(Set<String> caseIds) {
  }

  void report(TmsResult result);

  /**
   * Called once after all suites have finished.
   */
  default void finishRun() {
  }
}
