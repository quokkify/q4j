package dev.quokkify.testplanit.models;

import java.util.Objects;

/**
 * Settings of the run the results are reported to.
 *
 * @param projectId   project id
 * @param runId       existing run id or {@code null} to create a new run
 * @param runName     name of the new run
 * @param configId    configuration id or {@code null}
 * @param milestoneId milestone id or {@code null}
 * @param completeRun {@code true} to complete the run after all suites finish
 */
public record RunSettings(
    int projectId,
    Integer runId,
    String runName,
    Integer configId,
    Integer milestoneId,
    boolean completeRun) {

  public RunSettings {
    Objects.requireNonNull(runName, "runName");
  }
}
