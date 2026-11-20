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
        .contains("| OffstageTest.coveredButton | KNOWN_GAP | 800 | 3 |");
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
    assertThat(ComparisonReport.verdict(List.of(demo("flutter", FAILED, "x"), demo("native", FAILED, "x"))))
        .isEmpty();
    assertThat(ComparisonReport.verdict(List.of(demo("flutter", FAILED, "x"), demo("native", PASSED, "x"))))
        .hasValueSatisfying(reason -> assertThat(reason).contains("failure demo passed in native mode"));
    assertThat(ComparisonReport.verdict(List.of(scenario("flutter", FAILED)))).isPresent();
  }

  @Test
  public void verdictAcceptsPassedNotReachableAndKnownGap() {
    assertThat(ComparisonReport.verdict(List.of(
        scenario("flutter", PASSED),
        scenario("native", NOT_REACHABLE),
        new ScenarioResult("A.b", "native", KNOWN_GAP, 1, 1, false, null, "why"),
        demo("flutter", FAILED, "x"),
        demo("native", FAILED, "x")))).isEmpty();
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

  @Test
  public void verdictRequiresDiagnosticsForFailedDemo() {
    assertThat(ComparisonReport.verdict(List.of(demo("native", FAILED, null)))).hasValueSatisfying(reason ->
        assertThat(reason).isEqualTo(
            "failure demo FailureDemoTest.tapsMistypedKey in native mode captured no diagnostics"));
  }

  @Test
  public void verdictRequiresDemoInEveryModeThatRan() {
    assertThat(ComparisonReport.verdict(List.of(demo("flutter", FAILED, "x"), scenario("native", PASSED))))
        .hasValueSatisfying(reason -> assertThat(reason).isEqualTo(
            "failure demo FailureDemoTest.tapsMistypedKey has no result in native mode"));
    assertThat(ComparisonReport.verdict(List.of(demo("flutter", FAILED, "x"), scenario("flutter", PASSED))))
        .isEmpty();
  }

  @Test
  public void showsOnlyTheClauseOfTheRowsMode() {
    String description = "Login follows the form (known gap in flutter mode: flutter reason; in native mode: native"
        + " reason)";
    String md = ComparisonReport.render(List.of(
        new ScenarioResult("A.b", "flutter", KNOWN_GAP, 1, 1, false, null, description),
        new ScenarioResult("A.b", "native", KNOWN_GAP, 1, 1, false, null, description)));

    assertThat(md).contains("| flutter | A.b | Login follows the form (flutter reason) |")
        .contains("| native | A.b | Login follows the form (native reason) |");
  }

  @Test
  public void showsWholeDescriptionWithoutModeClauses() {
    String md = ComparisonReport.render(List.of(
        new ScenarioResult("A.b", "native", KNOWN_GAP, 1, 1, false, null, "plain reason")));

    assertThat(md).contains("| native | A.b | plain reason |");
  }

  @Test
  public void escapesPipesAndNewlinesInCells() {
    String md = ComparisonReport.render(List.of(
        new ScenarioResult("A.b", "native", KNOWN_GAP, 1, 1, false, null, "one | two\nthree")));

    assertThat(md).contains("| native | A.b | one \\| two three |");
  }

  @Test
  public void rendersEveryModeInRunOrder() {
    String md = ComparisonReport.render(List.of(
        new ScenarioResult("LoginTest.logsIn", "flutter", PASSED, 4200, 12, false, null),
        new ScenarioResult("LoginTest.logsIn", "flutter-sync", PASSED, 3000, 6, false, null),
        new ScenarioResult("LoginTest.logsIn", "native", PASSED, 3900, 9, false, null),
        new ScenarioResult("DuplicatesTest.opensCardB", "native", NOT_REACHABLE, 0, 0, false, null)));

    assertThat(md).contains("| Scenario | flutter | ms | finds | flutter-sync | ms | finds | native | ms | finds |\n"
            + "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n")
        .contains("| LoginTest.logsIn | PASSED | 4200 | 12 | PASSED | 3000 | 6 | PASSED | 3900 | 9 |")
        .contains("| DuplicatesTest.opensCardB | - | - | - | - | - | - | NOT_REACHABLE | 0 | 0 |");
  }

  @Test
  public void timesOnlyScenariosThatPassedInEveryMode() {
    String md = ComparisonReport.render(List.of(
        new ScenarioResult("A.one", "flutter", PASSED, 1000, 4, false, null),
        new ScenarioResult("A.one", "flutter-sync", PASSED, 600, 2, false, null),
        new ScenarioResult("A.one", "native", PASSED, 900, 3, false, null),
        new ScenarioResult("A.two", "flutter", PASSED, 500, 1, false, null),
        new ScenarioResult("A.two", "flutter-sync", PASSED, 500, 0, false, null),
        new ScenarioResult("A.two", "native", PASSED, 700, 2, false, null),
        new ScenarioResult("A.three", "flutter", PASSED, 9000, 9, false, null),
        new ScenarioResult("A.three", "flutter-sync", KNOWN_GAP, 9000, 9, false, null, "why"),
        new ScenarioResult("A.three", "native", PASSED, 9000, 9, false, null)));

    assertThat(md).contains("## Timing\n\nScenarios that passed in every mode: 2.")
        .contains("| Mode | ms | finds | ms per find |\n| --- | --- | --- | --- |\n"
            + "| flutter | 1500 | 5 | 300 |\n"
            + "| flutter-sync | 1100 | 2 | 550 |\n"
            + "| native | 1600 | 5 | 320 |\n");
  }

  @Test
  public void timingSaysWhenNoScenarioPassedEverywhere() {
    String md = ComparisonReport.render(List.of(scenario("flutter", PASSED), scenario("native", FAILED)));

    assertThat(md).contains("## Timing\n\nNo scenario passed in every mode.");
  }

  @Test
  public void flutterSyncSharesTheFlutterClauseAndShowsItsDemo() {
    String description = "Login follows the form (known gap in flutter mode: flutter reason; in native mode: native"
        + " reason)";
    String md = ComparisonReport.render(List.of(
        new ScenarioResult("A.b", "flutter-sync", KNOWN_GAP, 1, 1, false, null, description),
        demo("flutter", FAILED, "flutter excerpt"),
        demo("flutter-sync", FAILED, "sync excerpt"),
        demo("native", FAILED, "native excerpt")));

    assertThat(md).contains("| flutter-sync | A.b | Login follows the form (flutter reason) |")
        .contains("**flutter** (FAILED)\n\n```\nflutter excerpt")
        .contains("**flutter-sync** (FAILED)\n\n```\nsync excerpt")
        .contains("**native** (FAILED)\n\n```\nnative excerpt");
  }

  @Test
  public void verdictCoversThreeModes() {
    assertThat(ComparisonReport.verdict(List.of(
        scenario("flutter", PASSED),
        scenario("flutter-sync", PASSED),
        scenario("native", NOT_REACHABLE),
        new ScenarioResult("A.b", "flutter-sync", KNOWN_GAP, 1, 1, false, null, "why"),
        demo("flutter", FAILED, "x"),
        demo("flutter-sync", FAILED, "x"),
        demo("native", FAILED, "x")))).isEmpty();
    assertThat(ComparisonReport.verdict(List.of(
        demo("flutter", FAILED, "x"), demo("native", FAILED, "x"), scenario("flutter-sync", PASSED))))
        .hasValueSatisfying(reason -> assertThat(reason).isEqualTo(
            "failure demo FailureDemoTest.tapsMistypedKey has no result in flutter-sync mode"));
    assertThat(ComparisonReport.verdict(List.of(
        demo("flutter", FAILED, "x"), demo("flutter-sync", FAILED, null), demo("native", FAILED, "x"))))
        .hasValueSatisfying(reason -> assertThat(reason).isEqualTo(
            "failure demo FailureDemoTest.tapsMistypedKey in flutter-sync mode captured no diagnostics"));
    assertThat(ComparisonReport.verdict(List.of(scenario("flutter-sync", FAILED))))
        .hasValueSatisfying(reason -> assertThat(reason).isEqualTo("LoginTest.logsIn failed in flutter-sync mode"));
  }

  private static ScenarioResult scenario(String mode, Status status) {
    return new ScenarioResult("LoginTest.logsIn", mode, status, 10, 1, false, null);
  }

  private static ScenarioResult demo(String mode, Status status, String excerpt) {
    return new ScenarioResult("FailureDemoTest.tapsMistypedKey", mode, status, 10, 1, true, excerpt);
  }
}
