package dev.quokkify.testplanit.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.quokkify.testplanit.models.ResultSubmission;
import dev.quokkify.testplanit.models.RunCase;
import dev.quokkify.testplanit.models.TestPlanItRun;

/**
 * In-memory TestPlanIt with the statuses of a default project.
 */
final class FakeTestPlanItClient implements TestPlanItClient {

  final Map<Integer, TestPlanItRun> runs = new HashMap<>();
  final Map<Integer, Integer> caseVersions = new HashMap<>();
  final Map<String, Integer> statusIds = new HashMap<>(Map.of(
      "untested", 1, "passed", 2, "failed", 3, "retest", 4, "blocked", 5, "skipped", 6));
  final Map<Integer, Integer> existingResults = new HashMap<>();
  final Map<String, RunCase> runCases = new LinkedHashMap<>();
  final List<ResultSubmission> submitted = new ArrayList<>();
  final List<Integer> completedRuns = new ArrayList<>();
  int createdRuns;
  int addCaseCalls;
  String lastRunName;
  Integer lastConfigId;
  Integer lastMilestoneId;

  @Override
  public Optional<TestPlanItRun> findRun(int runId) {
    return Optional.ofNullable(runs.get(runId));
  }

  @Override
  public TestPlanItRun createRun(int projectId, String name, Integer configId, Integer milestoneId) {
    createdRuns++;
    lastRunName = name;
    lastConfigId = configId;
    lastMilestoneId = milestoneId;
    TestPlanItRun run = new TestPlanItRun(100 + createdRuns, projectId, name, false);
    runs.put(run.id(), run);
    return run;
  }

  @Override
  public Map<Integer, Integer> findCaseVersions(int projectId, Collection<Integer> caseIds) {
    Map<Integer, Integer> found = new HashMap<>();
    caseIds.stream().filter(caseVersions::containsKey).forEach(id -> found.put(id, caseVersions.get(id)));
    return found;
  }

  @Override
  public RunCase addCaseToRun(int runId, int caseId, int caseVersion) {
    addCaseCalls++;
    return runCases.computeIfAbsent(runId + ":" + caseId, key -> new RunCase(
        1000 + runCases.size(), caseId, caseVersion, existingResults.getOrDefault(caseId, 0)));
  }

  @Override
  public Map<String, Integer> findAutomationStatusIds(int projectId) {
    return statusIds;
  }

  @Override
  public void submitResult(ResultSubmission result) {
    submitted.add(result);
  }

  @Override
  public void completeRun(int runId, int projectId) {
    completedRuns.add(runId);
  }
}
