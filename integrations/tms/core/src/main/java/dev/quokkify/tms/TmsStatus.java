package dev.quokkify.tms;

/**
 * Result status reported to a test management system.
 */
public enum TmsStatus {
  PASSED,
  FAILED,
  SKIPPED,
  /**
   * The test was not executed on purpose, for example disabled via {@code @Test(enabled = false)}.
   */
  BLOCKED
}
