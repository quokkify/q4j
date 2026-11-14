package dev.quokkify.testplanit.services;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import dev.quokkify.testplanit.models.ResultSubmission;
import dev.quokkify.testplanit.models.RunCase;
import dev.quokkify.testplanit.models.TestPlanItRun;

public interface TestPlanItClient {

  Optional<TestPlanItRun> findRun(int runId);

  TestPlanItRun createRun(int projectId, String name, Integer configId, Integer milestoneId);

  /**
   * @return current versions of the given cases that exist in the project, keyed by case id
   */
  Map<Integer, Integer> findCaseVersions(int projectId, Collection<Integer> caseIds);

  /**
   * Adds the case to the run, or returns the run case if it is already there.
   */
  RunCase addCaseToRun(int runId, int caseId, int caseVersion);

  /**
   * @return ids of the statuses enabled for automation in the project, keyed by system name
   */
  Map<String, Integer> findAutomationStatusIds(int projectId);

  void submitResult(ResultSubmission result);

  void completeRun(int runId, int projectId);
}
