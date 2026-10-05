package dev.quokkify.architecture;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.quokkify.architecture.contract.RuleScope;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies how {@link ReportFormat} lays out the report in its plain and rich variants.
 */
public class ReportFormatTest {

  private static final String ESCAPE = "\u001B[";
  private static final String FINDING = "contract broken: SomeClass";

  private static final List<ReportFormat.Row> ROWS = List.of(
      new ReportFormat.Row("Green rule", RuleSeverity.ERROR, Set.of(RuleScope.TEST, RuleScope.MAIN), null, 312),
      new ReportFormat.Row("Loose rule with a longer name", RuleSeverity.WARNING, Set.of(), FINDING, 7));
  private static final ArchitectureRunner.Report REPORT = new ArchitectureRunner.Report(0, 1, 0);

  @Test
  public void plainReportAlignsRulesAndCarriesNoEscapeSequence() {
    List<String> lines = new ReportFormat(":common-utils:core", false, true).render(2, ROWS, REPORT, 1045);

    assertThat(lines).noneMatch(line -> line.contains(ESCAPE));
    assertThat(lines).contains("Architecture verification of :common-utils:core (2 rules)");
    assertThat(lines).contains(
        "[PASS]  Green rule                     MAIN+TEST  ERROR    312 ms",
        "[WARN]  Loose rule with a longer name  -          WARNING    7 ms",
        "    " + FINDING,
        "Architecture verification: 1 passed, 0 error(s), 1 warning(s), 0 info in 1045 ms");
  }

  @Test
  public void resourceScopeIsShownForResourceRules() {
    List<ReportFormat.Row> rows = List.of(
        new ReportFormat.Row("Service registrations resolve", RuleSeverity.ERROR, Set.of(RuleScope.RESOURCES), null, 1));

    assertThat(new ReportFormat("", false, true).render(1, rows, new ArchitectureRunner.Report(0, 0, 0), 1))
        .contains("[PASS]  Service registrations resolve  RESOURCES  ERROR    1 ms");
  }

  @Test
  public void plainReportOmitsTheModuleWhenItIsUnknown() {
    List<String> lines = new ReportFormat(null, false, true).render(2, ROWS, REPORT, 1045);

    assertThat(lines).contains("Architecture verification (2 rules)");
  }

  @Test
  public void richReportColorsEachRuleBySeverity() {
    String output = String.join("\n", new ReportFormat(":core", true, true).render(2, ROWS, REPORT, 1045));

    assertThat(output)
        .contains(ESCAPE)
        .contains("\u001B[32m✔")
        .contains("\u001B[33m⚠")
        .contains("│\u001B[0m " + FINDING)
        .contains("1 warning")
        .contains("1045 ms");
  }

  @Test
  public void richReportFallsBackToAsciiSymbolsOutsideUtf8() {
    String output = String.join("\n", new ReportFormat(":core", true, false).render(2, ROWS, REPORT, 1045));

    assertThat(output).doesNotContain("✔", "⚠", "│", "·", "─").contains("+", "!", "|");
  }

  @Test
  public void abortedRunIsReportedInBothLayouts() {
    List<ReportFormat.Row> evaluated = ROWS.subList(0, 1);
    ArchitectureRunner.Report clean = new ArchitectureRunner.Report(0, 0, 0);

    assertThat(new ReportFormat("", false, true).render(2, evaluated, clean, 10))
        .contains("Run aborted: 1 of 2 rules were not evaluated, see the error below.");
    assertThat(String.join("\n", new ReportFormat("", true, true).render(2, evaluated, clean, 10)))
        .contains("Run aborted: 1 of 2 rules were not evaluated");
  }

  @Test
  public void gateLineStaysParseableInPlainLayout() {
    ReportFormat plain = new ReportFormat("", false, true);

    assertThat(plain.gate(Optional.of(RuleSeverity.ERROR), false)).isEqualTo("Gate: fail on ERROR -> passed");
    assertThat(plain.gate(Optional.empty(), false)).isEqualTo("Gate: fail on NEVER -> passed");
    assertThat(new ReportFormat("", true, true).gate(Optional.of(RuleSeverity.WARNING), true))
        .contains("✘ Gate failed")
        .contains("(fail on WARNING)");
  }

  @Test
  public void colorModeFollowsTheTerminalOnlyInAuto() {
    assertThat(ReportFormat.parseColor("auto", true)).isTrue();
    assertThat(ReportFormat.parseColor(" AUTO ", false)).isFalse();
    assertThat(ReportFormat.parseColor("always", false)).isTrue();
    assertThat(ReportFormat.parseColor("never", true)).isFalse();
  }

  @Test
  public void unknownColorModeFailsLoudly() {
    assertThatThrownBy(() -> ReportFormat.parseColor("rainbow", true))
        .isInstanceOf(ArchitectureRunnerError.class)
        .hasMessageContaining("Unknown value 'rainbow'")
        .hasMessageContaining(ReportFormat.COLOR_PROPERTY);
  }
}
