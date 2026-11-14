package dev.quokkify.testplanit.models;

/**
 * Test run in TestPlanIt.
 *
 * @param id        run id
 * @param projectId project id
 * @param name      run name
 * @param completed {@code true} if the run is completed and no longer accepts results
 */
public record TestPlanItRun(int id, int projectId, String name, boolean completed) {
}
