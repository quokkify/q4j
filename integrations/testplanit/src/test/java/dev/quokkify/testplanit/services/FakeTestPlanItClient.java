package dev.quokkify.testplanit.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import dev.quokkify.testplanit.models.ResultSubmission;
import dev.quokkify.testplanit.models.RunCase;
import dev.quokkify.testplanit.models.TestPlanItRun;

/**
 * In-memory TestPlanIt with the statuses of a default project. Collections are concurrent so that
 * tests may call it from several threads; the scalar fields are written by single-threaded calls only.
 */
final class FakeTestPlanItClient implements TestPlanItClient {

  final Map<Integer, TestPlanItRun> runs = new ConcurrentHashMap<>();
  final Map<Integer, Integer> caseVersions = new ConcurrentHashMap<>();
  final Map<String, Integer> statusIds = new ConcurrentHashMap<>(Map.of(
      "untested", 1, "passed", 2, "failed", 3, "retest", 4, "blocked", 5, "skipped", 6));
  final Map<Integer, Integer> existingResults = new ConcurrentHashMap<>();
  final Map<String, RunCase> runCases = new ConcurrentHashMap<>();
  final List<ResultSubmission> submitted = new CopyOnWriteArrayList<>();
  final List<Integer> completedRuns = new CopyOnWriteArrayList<>();
  int createdRuns;
  final AtomicInteger addCaseCalls = new AtomicInteger();
  final AtomicInteger runCaseIds = new AtomicInteger(1000);
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
    addCaseCalls.incrementAndGet();
    return runCases.computeIfAbsent(runId + ":" + caseId, key -> new RunCase(
        runCaseIds.getAndIncrement(), caseId, caseVersion, existingResults.getOrDefault(caseId, 0)));
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
