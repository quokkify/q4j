package dev.quokkify.testplanit.services;

import java.time.Duration;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import dev.quokkify.testplanit.models.ResultSubmission;
import dev.quokkify.testplanit.models.RunCase;
import dev.quokkify.testplanit.models.RunSettings;
import dev.quokkify.testplanit.models.TestPlanItRun;
import dev.quokkify.tms.TmsResult;
import dev.quokkify.tms.TmsStatus;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reports results to one TestPlanIt run. The run is created once, or taken from {@link RunSettings#runId()};
 * each case is added to the run once, and every result becomes a new attempt of its run case.
 * Cases are never created: results of unknown case ids are skipped with a warning.
 */
public class TestPlanItService {

  private static final Logger LOG = LogManager.getLogger(TestPlanItService.class);
  private static final Map<TmsStatus, String> STATUS_SYSTEM_NAMES = new EnumMap<>(Map.of(
      TmsStatus.PASSED, "passed",
      TmsStatus.FAILED, "failed",
      TmsStatus.SKIPPED, "skipped",
      TmsStatus.BLOCKED, "blocked"));

  private final TestPlanItClient client;
  private final RunSettings settings;
  private final Map<Integer, RunCase> runCases = new ConcurrentHashMap<>();
  private final Map<Integer, AtomicInteger> attempts = new ConcurrentHashMap<>();
  private final Set<Integer> unknownCaseIds = ConcurrentHashMap.newKeySet();
  private volatile TestPlanItRun run;
  private volatile Map<TmsStatus, Integer> statusIds;

  public TestPlanItService(TestPlanItClient client, RunSettings settings) {
    this.client = Objects.requireNonNull(client, "client");
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  /**
   * Creates or loads the run and adds the given cases to it.
   *
   * @param caseIds TestPlanIt case ids
   */
  public synchronized void startRun(Collection<String> caseIds) {
    if (Objects.isNull(run)) {
      Map<TmsStatus, Integer> resolvedStatusIds =
          resolveStatusIds(client.findAutomationStatusIds(settings.projectId()));
      TestPlanItRun openedRun = openRun();
      statusIds = resolvedStatusIds;
      run = openedRun;
      LOG.info("Reporting results to TestPlanIt run {} '{}'", run.id(), run.name());
    }
    addCases(parseCaseIds(caseIds));
  }

  public void report(TmsResult result) {
    Optional<Integer> caseId = parseCaseId(result.caseId());
    if (caseId.isEmpty()) {
      return;
    }
    if (Objects.isNull(run) || !runCases.containsKey(caseId.get())) {
      startRun(Set.of(result.caseId()));
    }
    RunCase runCase = runCases.get(caseId.get());
    if (Objects.isNull(runCase)) {
      return;
    }
    int attempt = attempts
        .computeIfAbsent(runCase.id(), id -> new AtomicInteger(runCase.existingResults()))
        .incrementAndGet();
    client.submitResult(new ResultSubmission(
        run.id(),
        runCase.id(),
        statusIds.get(result.status()),
        attempt,
        runCase.caseVersion(),
        result.comment(),
        elapsedSeconds(result.elapsed())));
    LOG.debug("Reported {} for TestPlanIt case {} (attempt {})", result.status(), caseId.get(), attempt);
  }

  public synchronized void finishRun() {
    if (Objects.nonNull(run) && settings.completeRun()) {
      client.completeRun(run.id(), run.projectId());
      LOG.info("Completed TestPlanIt run {}", run.id());
    }
  }

  public Optional<TestPlanItRun> getRun() {
    return Optional.ofNullable(run);
  }

  /**
   * TestPlanIt stores whole seconds; a test that ran for a fraction of a second is reported as 1 second.
   */
  private static Long elapsedSeconds(Duration elapsed) {
    if (Objects.isNull(elapsed)) {
      return null;
    }
    long seconds = elapsed.toSeconds();
    return seconds == 0 && !elapsed.isZero() ? 1L : seconds;
  }

  private TestPlanItRun openRun() {
    if (Objects.isNull(settings.runId())) {
      return client.createRun(
          settings.projectId(), settings.runName(), settings.configId(), settings.milestoneId());
    }
    TestPlanItRun existing = client.findRun(settings.runId())
        .orElseThrow(() -> new TestPlanItException("TestPlanIt run %d not found".formatted(settings.runId())));
    if (existing.projectId() != settings.projectId()) {
      throw new TestPlanItException("TestPlanIt run %d belongs to project %d, not to project %d"
          .formatted(existing.id(), existing.projectId(), settings.projectId()));
    }
    if (existing.completed()) {
      throw new TestPlanItException("TestPlanIt run %d is completed and does not accept results"
          .formatted(existing.id()));
    }
    return existing;
  }

  private void addCases(Set<Integer> caseIds) {
    Set<Integer> newCaseIds = new LinkedHashSet<>(caseIds);
    newCaseIds.removeAll(runCases.keySet());
    newCaseIds.removeAll(unknownCaseIds);
    if (newCaseIds.isEmpty()) {
      return;
    }
    Map<Integer, Integer> versions = client.findCaseVersions(settings.projectId(), newCaseIds);
    newCaseIds.forEach(caseId -> {
      Integer version = versions.get(caseId);
      if (Objects.isNull(version)) {
        unknownCaseIds.add(caseId);
        LOG.warn("TestPlanIt case {} does not exist in project {}, its results are skipped",
            caseId, settings.projectId());
      } else {
        runCases.put(caseId, client.addCaseToRun(run.id(), caseId, version));
      }
    });
  }

  private static Map<TmsStatus, Integer> resolveStatusIds(Map<String, Integer> idsBySystemName) {
    Map<TmsStatus, Integer> ids = new EnumMap<>(TmsStatus.class);
    STATUS_SYSTEM_NAMES.forEach((status, systemName) -> {
      Integer id = idsBySystemName.get(systemName);
      if (Objects.isNull(id)) {
        throw new TestPlanItException(
            "Status '%s' is not enabled for automation in the TestPlanIt project".formatted(systemName));
      }
      ids.put(status, id);
    });
    return ids;
  }

  private static Set<Integer> parseCaseIds(Collection<String> caseIds) {
    Set<Integer> ids = new LinkedHashSet<>();
    caseIds.forEach(caseId -> parseCaseId(caseId).ifPresent(ids::add));
    return ids;
  }

  private static Optional<Integer> parseCaseId(String caseId) {
    String trimmed = caseId.trim();
    if (NumberUtils.isDigits(trimmed)) {
      int id = NumberUtils.toInt(trimmed, 0);
      if (id > 0) {
        return Optional.of(id);
      }
    }
    LOG.warn("'{}' is not a TestPlanIt case id, expected a positive number that fits an int", caseId);
    return Optional.empty();
  }
}
