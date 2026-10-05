package dev.quokkify.architecture.taikai;

import java.util.List;

import dev.quokkify.architecture.ArchitectureRunner;
import dev.quokkify.architecture.RunnerLogCapture;
import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;
import dev.quokkify.architecture.fixtures.taikai.violating.LowercaseLogger;

import com.enofex.taikai.Taikai;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TaikaiArchitectureRuleTest {

  private static final String FIXTURES = "dev.quokkify.architecture.fixtures.taikai.";

  @Test
  public void taikaiViolationsAreReportedAsOneFindingUnderTheRuleSeverity() {
    ArchitectureRunner.Report report;
    String output;
    try (RunnerLogCapture capture = RunnerLogCapture.open();
         ArchitectureContext context = contextFor("violating")) {
      report = ArchitectureRunner.verify(List.of(new LoggerNamingRule(RuleSeverity.WARNING)), context);
      output = capture.output();
    }

    assertThat(report).isEqualTo(new ArchitectureRunner.Report(0, 1, 0));
    assertThat(output)
        .contains("Found 1 Taikai violations")
        .contains(LowercaseLogger.class.getName());
  }

  @Test
  public void satisfiedTaikaiRulesPass() {
    try (ArchitectureContext context = contextFor("clean")) {
      assertThatCode(() -> new LoggerNamingRule(RuleSeverity.ERROR).verify(context)).doesNotThrowAnyException();
    }
  }

  @Test
  public void ruleSetWithoutTaikaiRulesCannotRun() {
    TaikaiArchitectureRule empty = new TestRule(RuleSeverity.ERROR) {

      @Override
      protected void configure(Taikai.Builder builder) {
        // Deliberately configures nothing.
      }
    };

    try (ArchitectureContext context = contextFor("clean")) {
      assertThatThrownBy(() -> empty.verify(context))
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining("configures no Taikai rule");
    }
  }

  @Test
  public void emptyImportCannotRunInsteadOfBeingAViolation() {
    try (ArchitectureContext context = new ArchitectureContext(List.of(FIXTURES + "missing"))) {
      assertThatThrownBy(() -> new LoggerNamingRule(RuleSeverity.WARNING).verify(context))
          .as("an empty import must abort the run, not become a WARNING finding")
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining("imported no class");
    }
  }

  private static ArchitectureContext contextFor(String fixture) {
    return new ArchitectureContext(List.of(FIXTURES + fixture));
  }

  private static class LoggerNamingRule extends TestRule {

    LoggerNamingRule(RuleSeverity severity) {
      super(severity);
    }

    @Override
    protected void configure(Taikai.Builder builder) {
      builder.logging(logging -> logging.loggersShouldFollowConventions("org.apache.logging.log4j.Logger", "LOG"));
    }
  }

  private abstract static class TestRule extends TaikaiArchitectureRule {

    private final RuleSeverity severity;

    TestRule(RuleSeverity severity) {
      this.severity = severity;
    }

    @Override
    public String name() {
      return "taikai test rule";
    }

    @Override
    public RuleSeverity severity() {
      return severity;
    }
  }
}
