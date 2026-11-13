package dev.quokkify.testplanit.services;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import dev.quokkify.testplanit.models.ResultSubmission;
import dev.quokkify.testplanit.models.RunSettings;
import dev.quokkify.testplanit.models.TestPlanItRun;
import dev.quokkify.tms.TmsResult;
import dev.quokkify.tms.TmsStatus;

import io.qameta.allure.TmsLink;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestPlanItServiceTest {

  private static final int PROJECT_ID = 1;

  private FakeTestPlanItClient client;

  @BeforeMethod
  public void setUp() {
    client = new FakeTestPlanItClient();
    client.caseVersions.put(6, 1);
    client.caseVersions.put(7, 3);
  }

  @TmsLink("TPI_SERVICE_1")
  @Test(description = "One run is created with the configured name, configuration and milestone")
  public void createsRunOnce() {
    TestPlanItService service = new TestPlanItService(client, new RunSettings(PROJECT_ID, null, "nightly", 2, 3, false));

    service.startRun(Set.of("6"));
    service.startRun(Set.of("7"));

    assertThat(client.createdRuns).as("Created runs").isEqualTo(1);
    assertThat(client.lastRunName).isEqualTo("nightly");
    assertThat(client.lastConfigId).isEqualTo(2);
    assertThat(client.lastMilestoneId).isEqualTo(3);
    assertThat(service.getRun()).map(TestPlanItRun::id).contains(101);
  }

  @TmsLink("TPI_SERVICE_2")
  @Test(description = "Results are appended to the configured run instead of a new one")
  public void reusesConfiguredRun() {
    client.runs.put(55, new TestPlanItRun(55, PROJECT_ID, "existing", false));
    TestPlanItService service = new TestPlanItService(client, settings(55, false));

    service.report(new TmsResult("6", TmsStatus.PASSED, null, null));

    assertThat(client.createdRuns).isZero();
    assertThat(client.submitted).singleElement().extracting(ResultSubmission::runId).isEqualTo(55);
  }

  @TmsLink("TPI_SERVICE_3")
  @Test(description = "A configured run that is missing, completed or in another project is rejected")
  public void rejectsUnusableConfiguredRun() {
    client.runs.put(56, new TestPlanItRun(56, PROJECT_ID, "done", true));
    client.runs.put(57, new TestPlanItRun(57, 2, "other project", false));

    assertThatThrownBy(() -> new TestPlanItService(client, settings(404, false)).startRun(Set.of()))
        .isInstanceOf(TestPlanItException.class).hasMessageContaining("run 404 not found");
    assertThatThrownBy(() -> new TestPlanItService(client, settings(56, false)).startRun(Set.of()))
        .isInstanceOf(TestPlanItException.class).hasMessageContaining("is completed");
    assertThatThrownBy(() -> new TestPlanItService(client, settings(57, false)).startRun(Set.of()))
        .isInstanceOf(TestPlanItException.class).hasMessageContaining("belongs to project 2");
  }

  @TmsLink("TPI_SERVICE_4")
  @Test(description = "A repeated result of a case adds an attempt, not a second run case")
  public void repeatedResultAddsAttempt() {
    TestPlanItService service = new TestPlanItService(client, settings(null, false));
    service.startRun(Set.of("6", "7"));

    service.report(new TmsResult("6", TmsStatus.FAILED, "boom", Duration.ofMillis(2_400)));
    service.report(new TmsResult("6", TmsStatus.PASSED, null, Duration.ofSeconds(1)));

    assertThat(client.addCaseCalls).as("Run cases added").isEqualTo(2);
    assertThat(client.submitted).extracting(ResultSubmission::attempt).containsExactly(1, 2);
    assertThat(client.submitted).extracting(ResultSubmission::runCaseId)
        .containsOnly(client.runCases.get("101:6").id());
  }

  @TmsLink("TPI_SERVICE_5")
  @Test(description = "Attempts continue after the results a run case already has")
  public void continuesExistingAttempts() {
    client.existingResults.put(6, 2);
    TestPlanItService service = new TestPlanItService(client, settings(null, false));

    service.report(new TmsResult("6", TmsStatus.PASSED, null, null));

    assertThat(client.submitted).singleElement().extracting(ResultSubmission::attempt).isEqualTo(3);
  }

  @TmsLink("TPI_SERVICE_13")
  @Test(description = "Regression guard: a case id that does not fit an int is skipped, not fatal for the suite")
  public void skipsCaseIdOutOfIntRange() {
    TestPlanItService service = new TestPlanItService(client, settings(null, false));

    assertThatCode(() -> service.startRun(Set.of("6", "99999999999"))).doesNotThrowAnyException();
    service.report(new TmsResult("6", TmsStatus.PASSED, null, null));

    assertThat(client.submitted).singleElement().extracting(ResultSubmission::runCaseId)
        .isEqualTo(client.runCases.get("101:6").id());
  }

  @TmsLink("TPI_SERVICE_6")
  @Test(description = "Statuses map to the project status ids; notes, elapsed seconds and case version are sent")
  public void mapsResultFields() {
    TestPlanItService service = new TestPlanItService(client, settings(null, false));

    List.of(TmsStatus.PASSED, TmsStatus.FAILED, TmsStatus.SKIPPED, TmsStatus.BLOCKED)
        .forEach(status -> service.report(new TmsResult("7", status, "note", Duration.ofMillis(3_900))));

    assertThat(client.submitted).extracting(ResultSubmission::statusId).containsExactly(2, 3, 6, 5);
    assertThat(client.submitted).allSatisfy(result -> {
      assertThat(result.notes()).isEqualTo("note");
      assertThat(result.elapsedSeconds()).isEqualTo(3L);
      assertThat(result.caseVersion()).isEqualTo(3);
    });
  }

  @TmsLink("TPI_SERVICE_7")
  @Test(description = "A case id that does not exist in the project is skipped and looked up once")
  public void skipsUnknownCase() {
    TestPlanItService service = new TestPlanItService(client, settings(null, false));
    service.startRun(Set.of("6", "999"));

    service.report(new TmsResult("999", TmsStatus.PASSED, null, null));
    service.report(new TmsResult("not-a-number", TmsStatus.PASSED, null, null));

    assertThat(client.submitted).isEmpty();
    assertThat(client.runCases).hasSize(1);
  }

  @TmsLink("TPI_SERVICE_8")
  @Test(description = "The run is completed only when completeRun is enabled")
  public void completesRunOnlyWhenEnabled() {
    TestPlanItService keepOpen = new TestPlanItService(client, settings(null, false));
    keepOpen.startRun(Set.of("6"));
    keepOpen.finishRun();
    TestPlanItService complete = new TestPlanItService(client, settings(null, true));
    complete.startRun(Set.of("6"));
    complete.finishRun();

    assertThat(client.completedRuns).containsExactly(102);
  }

  @TmsLink("TPI_SERVICE_9")
  @Test(description = "A run is not started when a required status is not enabled for automation")
  public void failsWithoutRequiredStatus() {
    client.statusIds.remove("skipped");
    TestPlanItService service = new TestPlanItService(client, settings(null, false));

    assertThatThrownBy(() -> service.startRun(Set.of("6")))
        .isInstanceOf(TestPlanItException.class)
        .hasMessageContaining("'skipped' is not enabled for automation");
    assertThat(client.createdRuns).as("No empty run is left behind").isZero();
    assertThat(service.getRun()).isEmpty();
  }

  @TmsLink("TPI_SERVICE_11")
  @Test(description = "A sub-second test is reported as 1 second, a zero duration as 0")
  public void roundsSubSecondElapsedUp() {
    TestPlanItService service = new TestPlanItService(client, settings(null, false));

    service.report(new TmsResult("6", TmsStatus.PASSED, null, Duration.ofMillis(120)));
    service.report(new TmsResult("6", TmsStatus.PASSED, null, Duration.ZERO));

    assertThat(client.submitted).extracting(ResultSubmission::elapsedSeconds).containsExactly(1L, 0L);
  }

  @TmsLink("TPI_SERVICE_10")
  @Test(description = "Nothing is created when the run is never used")
  public void finishWithoutRunDoesNothing() {
    new TestPlanItService(client, settings(null, true)).finishRun();

    assertThat(client.createdRuns).isZero();
    assertThat(client.completedRuns).isEmpty();
  }

  @TmsLink("TPI_SERVICE_12")
  @Test(description = "A non-numeric case id from another TMS is not reported and the client is not called")
  public void nonNumericCaseIdIsNotReported() {
    TestPlanItService service = new TestPlanItService(client, settings(null, false));

    service.report(new TmsResult("JWT_ID_1", TmsStatus.PASSED, null, null));

    assertThat(client.submitted).isEmpty();
    assertThat(client.createdRuns).isZero();
  }

  private static RunSettings settings(Integer runId, boolean completeRun) {
    return new RunSettings(PROJECT_ID, runId, "q4j", null, null, completeRun);
  }
}
