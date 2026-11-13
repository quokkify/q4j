package dev.quokkify.tms;

import java.time.Duration;
import java.util.Objects;

/**
 * Result of one test case to report to a test management system.
 *
 * @param caseId  case id in the format of the selected system
 * @param status  result status
 * @param comment free-text comment, may be {@code null}
 * @param elapsed execution time, may be {@code null}
 */
public record TmsResult(String caseId, TmsStatus status, String comment, Duration elapsed) {

  public TmsResult {
    Objects.requireNonNull(caseId, "caseId");
    Objects.requireNonNull(status, "status");
  }
}
