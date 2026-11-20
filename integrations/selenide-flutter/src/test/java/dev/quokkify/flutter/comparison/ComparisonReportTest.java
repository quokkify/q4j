package dev.quokkify.flutter.comparison;

import java.util.List;

import org.testng.annotations.Test;

import static dev.quokkify.flutter.comparison.Status.FAILED;
import static dev.quokkify.flutter.comparison.Status.KNOWN_GAP;
import static dev.quokkify.flutter.comparison.Status.NOT_REACHABLE;
import static dev.quokkify.flutter.comparison.Status.PASSED;
import static org.assertj.core.api.Assertions.assertThat;

public class ComparisonReportTest {

  @Test
  public void rendersBothModesPerScenario() {
    String md = ComparisonReport.render(List.of(
        new ScenarioResult("LoginTest.logsIn", "flutter", PASSED, 4200, 12, false, null),
        new ScenarioResult("LoginTest.logsIn", "native", PASSED, 3900, 9, false, null),
        new ScenarioResult("DuplicatesTest.opensCardB", "native", NOT_REACHABLE, 0, 0, false, null)));

    assertThat(md).contains("| LoginTest.logsIn | PASSED | 4200 | 12 | PASSED | 3900 | 9 |");
    assertThat(md).contains("| DuplicatesTest.opensCardB | - | - | - | NOT_REACHABLE | 0 | 0 |");
  }

  @Test
  public void rendersFailureDemoDiagnosticsSideBySide() {
    String md = ComparisonReport.render(List.of(
        demo("flutter", FAILED, "0.91 Text key=login_button"),
        demo("native", FAILED, "<hierarchy rotation=\"0\">")));

    assertThat(md).contains("## Failure demo").contains("0.91 Text key=login_button")
        .contains("<hierarchy rotation=\"0\">");
  }

  @Test
  public void listsKnownGapsWithReason() {
    String md = ComparisonReport.render(List.of(
        new ScenarioResult("OffstageTest.coveredButton", "native", KNOWN_GAP, 800, 3, false, null,
            "A button under an opaque cover is not visible")));

    assertThat(md).contains("## Known gaps")
        .contains("| native | OffstageTest.coveredButton | A button under an opaque cover is not visible |")
        .contains("| OffstageTest.coveredButton | - | - | - | KNOWN_GAP | 800 | 3 |");
  }

  @Test
  public void flagsKnownGapThatDidNotReproduce() {
    ScenarioResult passed = new ScenarioResult("DelayedWidgetTest.shows", "flutter", PASSED, 100, 1, false, null,
        "slow widget");

    assertThat(ComparisonReport.render(List.of(passed))).contains("known gap did not reproduce")
        .contains("DelayedWidgetTest.shows");
    assertThat(ComparisonReport.verdict(List.of(passed))).isEmpty();
  }

  @Test
  public void verdictRequiresDemoToFailInBothModes() {
    assertThat(ComparisonReport.verdict(List.of(demo("flutter", FAILED, null), demo("native", FAILED, null))))
        .isEmpty();
    assertThat(ComparisonReport.verdict(List.of(demo("flutter", FAILED, null), demo("native", PASSED, null))))
        .hasValueSatisfying(reason -> assertThat(reason).contains("failure demo passed in native mode"));
    assertThat(ComparisonReport.verdict(List.of(scenario("flutter", FAILED)))).isPresent();
  }

  @Test
  public void verdictAcceptsPassedNotReachableAndKnownGap() {
    assertThat(ComparisonReport.verdict(List.of(
        scenario("flutter", PASSED),
        scenario("native", NOT_REACHABLE),
        new ScenarioResult("A.b", "native", KNOWN_GAP, 1, 1, false, null, "why"),
        demo("flutter", FAILED, null),
        demo("native", FAILED, null)))).isEmpty();
  }

  @Test
  public void verdictNamesTheFailedScenario() {
    assertThat(ComparisonReport.verdict(List.of(scenario("native", FAILED))))
        .hasValueSatisfying(reason -> assertThat(reason).contains("LoginTest.logsIn failed in native mode")
            .doesNotContain("\n"));
  }

  @Test
  public void verdictRejectsNoResults() {
    assertThat(ComparisonReport.verdict(List.of())).hasValueSatisfying(reason -> assertThat(reason)
        .contains("no results"));
  }

  private static ScenarioResult scenario(String mode, Status status) {
    return new ScenarioResult("LoginTest.logsIn", mode, status, 10, 1, false, null);
  }

  private static ScenarioResult demo(String mode, Status status, String excerpt) {
    return new ScenarioResult("FailureDemoTest.tapsMistypedKey", mode, status, 10, 1, true, excerpt);
  }
}
