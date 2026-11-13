package dev.quokkify.testplanit.models;

/**
 * Repository case added to a run.
 *
 * @param id              run case id
 * @param caseId          repository case id
 * @param caseVersion     current version of the repository case
 * @param existingResults number of results the run case already has
 */
public record RunCase(int id, int caseId, int caseVersion, int existingResults) {
}
