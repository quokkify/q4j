package dev.quokkify.testplanit.models;

/**
 * Result of a run case submitted to TestPlanIt.
 *
 * @param runId          run id
 * @param runCaseId      run case id
 * @param statusId       TestPlanIt status id
 * @param attempt        attempt number, starts with 1
 * @param caseVersion    repository case version the result was produced for
 * @param notes          plain-text notes, may be {@code null}
 * @param elapsedSeconds execution time in seconds, may be {@code null}
 */
public record ResultSubmission(
    int runId,
    int runCaseId,
    int statusId,
    int attempt,
    int caseVersion,
    String notes,
    Long elapsedSeconds) {
}
