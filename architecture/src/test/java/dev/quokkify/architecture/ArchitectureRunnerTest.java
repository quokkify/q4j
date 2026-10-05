package dev.quokkify.architecture;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.ArchitectureRule;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;
import dev.quokkify.architecture.exceptions.ArchitectureViolationException;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies how {@link ArchitectureRunner} reports and aggregates rule results per severity.
 */
public class ArchitectureRunnerTest {

  private static final String BOOM = "contract broken: SomeClass";

  private static final int CONCURRENCY_TIMEOUT_SECONDS = 10;
  private static final long SLOW_RULE_MILLIS = 200;
  private static final List<String> PACKAGES = List.of("dev.quokkify.architecture.fixtures");

  @Test
  public void passingRuleIsReportedAsPassedAndDoesNotFailTheBuild() {
    Outcome outcome = run(satisfied("green rule", RuleSeverity.ERROR));

    assertThat(outcome.report().hasFindingAtOrAbove(RuleSeverity.ERROR)).isFalse();
    assertThat(outcome.report()).isEqualTo(new ArchitectureRunner.Report(0, 0, 0));
    assertThat(outcome.output()).contains("PASS").contains("green rule");
  }

  @Test
  public void warningRuleIsReportedAsWarningAndDoesNotFailTheBuild() {
    Outcome outcome = run(violated("loose rule", RuleSeverity.WARNING));

    assertThat(outcome.report().hasFindingAtOrAbove(RuleSeverity.ERROR)).isFalse();
    assertThat(outcome.report().warnings()).isEqualTo(1);
    assertThat(outcome.output()).contains("WARN").contains("loose rule").contains(BOOM);
  }

  @Test
  public void infoRuleIsReportedAsInfoAndDoesNotFailTheBuild() {
    Outcome outcome = run(violated("advisory rule", RuleSeverity.INFO));

    assertThat(outcome.report().hasFindingAtOrAbove(RuleSeverity.ERROR)).isFalse();
    assertThat(outcome.report().infos()).isEqualTo(1);
    assertThat(outcome.output()).contains("INFO").contains("advisory rule").contains(BOOM);
  }

  @Test
  public void errorRuleFailsTheBuild() {
    Outcome outcome = run(violated("mandatory rule", RuleSeverity.ERROR));

    assertThat(outcome.report().hasFindingAtOrAbove(RuleSeverity.ERROR)).isTrue();
    assertThat(outcome.report().errors()).isEqualTo(1);
    assertThat(outcome.output()).contains("ERROR").contains("mandatory rule").contains(BOOM);
  }

  @Test
  public void everyRuleIsEvaluatedEvenWhenAnotherRuleIsViolated() {
    // Concurrent, so the recorder must be thread safe and the completion order is not asserted. The
    // reported order is asserted separately by reportOrderFollowsTheRuleListNotCompletion.
    Collection<String> evaluated = new ConcurrentLinkedQueue<>();

    Outcome outcome = run(
        recording(evaluated, violated("first warning", RuleSeverity.WARNING)),
        recording(evaluated, violated("second error", RuleSeverity.ERROR)),
        recording(evaluated, satisfied("third ok", RuleSeverity.ERROR)));

    assertThat(evaluated).containsExactlyInAnyOrder("first warning", "second error", "third ok");
    assertThat(outcome.report()).isEqualTo(new ArchitectureRunner.Report(1, 1, 0));
    assertThat(outcome.report().hasFindingAtOrAbove(RuleSeverity.ERROR)).isTrue();
  }

  @Test
  public void rulesAreEvaluatedConcurrently() {
    // Each rule blocks until every rule has started. Sequential evaluation cannot get past the first one,
    // so the latch times out and the assertion below fails: this is what proves the rules really overlap.
    int ruleCount = 4;
    CountDownLatch started = new CountDownLatch(ruleCount);
    Collection<Boolean> allStarted = new ConcurrentLinkedQueue<>();
    ArchitectureRule[] rules = new ArchitectureRule[ruleCount];
    for (int index = 0; index < ruleCount; index++) {
      rules[index] = rule("concurrent rule %d".formatted(index), RuleSeverity.ERROR, () -> {
        started.countDown();
        try {
          allStarted.add(started.await(CONCURRENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          allStarted.add(false);
        }
      });
    }

    Outcome outcome = run(rules);

    assertThat(allStarted)
        .as("every rule must observe all the others already started, which only holds if they run in parallel")
        .hasSize(ruleCount)
        .containsOnly(true);
    assertThat(outcome.report()).isEqualTo(new ArchitectureRunner.Report(0, 0, 0));
  }

  @Test
  public void reportOrderFollowsTheRuleListNotCompletion() {
    // The slowest rule is submitted first, so completion order is the reverse of the list order.
    Outcome outcome = run(
        rule("slow rule", RuleSeverity.ERROR, () -> sleep(SLOW_RULE_MILLIS)),
        satisfied("fast rule", RuleSeverity.ERROR));

    assertThat(outcome.output()).containsSubsequence("slow rule", "fast rule");
  }

  @Test
  public void warningsAndInfosNeverDowngradeAnError() {
    Outcome outcome = run(
        violated("info", RuleSeverity.INFO),
        violated("warning", RuleSeverity.WARNING),
        violated("error", RuleSeverity.ERROR));

    assertThat(outcome.report().hasFindingAtOrAbove(RuleSeverity.ERROR)).isTrue();
    assertThat(outcome.report()).isEqualTo(new ArchitectureRunner.Report(1, 1, 1));
  }

  @Test
  public void ruleThrowingWithoutMessageIsStillReported() {
    ArchitectureRule silent = rule("silent rule", RuleSeverity.ERROR, () -> {
      throw new IllegalStateException();
    });

    Outcome outcome = run(silent);

    assertThat(outcome.report().hasFindingAtOrAbove(RuleSeverity.ERROR)).isTrue();
    assertThat(outcome.output()).contains("java.lang.IllegalStateException was thrown without a message");
  }

  @Test
  public void archUnitAssertionErrorIsReportedAsViolationAndNotPropagated() {
    ArchitectureRule archUnitStyle = rule("archunit rule", RuleSeverity.ERROR, () -> {
      throw new AssertionError("Architecture Violation [Priority: MEDIUM] - Rule 'x' was violated (1 times)");
    });

    Outcome outcome = run(archUnitStyle);

    assertThat(outcome.report().errors()).isEqualTo(1);
    assertThat(outcome.output()).contains("Architecture Violation").contains("was violated (1 times)");
  }

  @Test
  public void ruleThatCannotRunFailsTheBuildRegardlessOfItsSeverity() {
    ArchitectureRule cannotRun = rule("unverifiable rule", RuleSeverity.WARNING, () -> {
      throw new ArchitectureRunnerError("classpath element missing");
    });

    assertThatThrownBy(() -> run(cannotRun))
        .as("an inability to verify must not be downgraded to this rule's WARNING severity")
        .isInstanceOf(ArchitectureRunnerError.class)
        .hasMessageContaining("classpath element missing");
  }

  @Test
  public void abortedRunStillReportsWhatEarlierRulesFound() {
    ArchitectureRule cannotRun = rule("unverifiable rule", RuleSeverity.ERROR, () -> {
      throw new ArchitectureRunnerError("classpath element missing");
    });
    String output;
    try (RunnerLogCapture capture = RunnerLogCapture.open();
         ArchitectureContext context = new ArchitectureContext(PACKAGES)) {
      assertThatThrownBy(() -> ArchitectureRunner.verify(
          List.of(violated("earlier finding", RuleSeverity.WARNING), cannotRun), context))
          .isInstanceOf(ArchitectureRunnerError.class);
      output = capture.output();
    }

    assertThat(output)
        .as("the findings of the rules that did complete must survive an aborted run")
        .contains("earlier finding")
        .contains(BOOM)
        .contains("Run aborted");
  }

  @Test
  public void gateThresholdDefaultsToWarningAndAcceptsEverySeverity() {
    assertThat(ArchitectureRunner.parseFailOn("ERROR")).contains(RuleSeverity.ERROR);
    assertThat(ArchitectureRunner.parseFailOn("WARNING")).contains(RuleSeverity.WARNING);
    assertThat(ArchitectureRunner.parseFailOn(" info ")).contains(RuleSeverity.INFO);
    assertThat(ArchitectureRunner.parseFailOn("never")).isEmpty();
    assertThat(ArchitectureRunner.failOnThreshold())
        .as("errors and warnings must both fail the build by default")
        .contains(RuleSeverity.WARNING);
  }

  @Test
  public void unknownGateThresholdFailsLoudly() {
    assertThatThrownBy(() -> ArchitectureRunner.parseFailOn("blocker"))
        .isInstanceOf(ArchitectureRunnerError.class)
        .hasMessageContaining("Unknown value 'blocker'")
        .hasMessageContaining(ArchitectureRunner.FAIL_ON_PROPERTY);
  }

  @Test
  public void warningFailsTheGateOnlyWhenTheThresholdAllowsIt() {
    ArchitectureRunner.Report warningOnly = new ArchitectureRunner.Report(0, 1, 0);

    assertThat(warningOnly.hasFindingAtOrAbove(ArchitectureRunner.DEFAULT_FAIL_ON))
        .as("a warning must fail the default gate")
        .isTrue();
    assertThat(warningOnly.hasFindingAtOrAbove(RuleSeverity.ERROR))
        .as("lowering the gate to ERROR must let the same finding through")
        .isFalse();
    assertThat(warningOnly.hasFindingAtOrAbove(RuleSeverity.INFO)).isTrue();
  }

  @Test
  public void errorFailsTheGateAtEveryThreshold() {
    ArchitectureRunner.Report errorOnly = new ArchitectureRunner.Report(1, 0, 0);

    assertThat(errorOnly.hasFindingAtOrAbove(RuleSeverity.ERROR)).isTrue();
    assertThat(errorOnly.hasFindingAtOrAbove(RuleSeverity.WARNING)).isTrue();
    assertThat(errorOnly.hasFindingAtOrAbove(RuleSeverity.INFO)).isTrue();
  }

  @Test
  public void cleanRunFailsNoThreshold() {
    ArchitectureRunner.Report clean = new ArchitectureRunner.Report(0, 0, 0);

    assertThat(clean.hasFindingAtOrAbove(RuleSeverity.INFO)).isFalse();
  }

  @Test
  public void emptyRuleSetAbortsTheRunInsteadOfPassing() {
    assertThatThrownBy(ArchitectureRunner::discoverRules)
        .as("this module registers no rule of its own, and a run that verifies nothing must not pass")
        .isInstanceOf(ArchitectureRunnerError.class)
        .hasMessageContaining("No ArchitectureRule was discovered");
  }

  private static Outcome run(ArchitectureRule... rules) {
    try (RunnerLogCapture capture = RunnerLogCapture.open();
         ArchitectureContext context = new ArchitectureContext(PACKAGES)) {
      ArchitectureRunner.Report report = ArchitectureRunner.verify(List.of(rules), context);
      return new Outcome(report, capture.output());
    }
  }

  private static ArchitectureRule satisfied(String name, RuleSeverity severity) {
    return rule(name, severity, () -> {
    });
  }

  private static ArchitectureRule violated(String name, RuleSeverity severity) {
    return rule(name, severity, () -> {
      throw new ArchitectureViolationException(BOOM);
    });
  }

  private static ArchitectureRule recording(Collection<String> evaluated, ArchitectureRule delegate) {
    return rule(delegate.name(), delegate.severity(), () -> {
      evaluated.add(delegate.name());
      delegate.verify(null);
    });
  }

  private static ArchitectureRule rule(String name, RuleSeverity severity, Runnable body) {
    return new ArchitectureRule() {

      @Override
      public String name() {
        return name;
      }

      @Override
      public RuleSeverity severity() {
        return severity;
      }

      @Override
      public void verify(ArchitectureContext context) {
        body.run();
      }
    };
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  private record Outcome(ArchitectureRunner.Report report, String output) {
  }
}
