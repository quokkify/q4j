package dev.quokkify.architecture.rules;

import java.util.List;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;
import dev.quokkify.architecture.exceptions.ArchitectureViolationException;
import dev.quokkify.architecture.fixtures.naming.compliant.CompliantSteps;
import dev.quokkify.architecture.fixtures.naming.compliant.CompliantTest;
import dev.quokkify.architecture.fixtures.naming.violating.MisnamedClassLevelCase;
import dev.quokkify.architecture.fixtures.naming.violating.MisnamedMethodCase;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestClassNamingRuleTest {

  private static final String FIXTURES = "dev.quokkify.architecture.fixtures.naming.";

  private final TestClassNamingRule rule = new TestClassNamingRule();

  @Test
  public void correctlyNamedTestClassesAndSupportClassesPass() {
    try (ArchitectureContext context = contextFor("compliant")) {
      assertThatCode(() -> rule.verify(context)).doesNotThrowAnyException();
      assertThat(context.scan().getAllClasses().getNames())
          .as("the support class must be scanned and exempted, not missed")
          .contains(CompliantTest.class.getName(), CompliantSteps.class.getName());
    }
  }

  @Test
  public void methodAndClassLevelTestOnMisnamedClassesAreReported() {
    try (ArchitectureContext context = contextFor("violating")) {
      assertThatThrownBy(() -> rule.verify(context))
          .isInstanceOf(ArchitectureViolationException.class)
          .hasMessageContaining("Violations (2)")
          .hasMessageContaining(MisnamedMethodCase.class.getName())
          .hasMessageContaining(MisnamedClassLevelCase.class.getName());
    }
  }

  @Test
  public void noTestClassOnTheClasspathMeansTheRuleCannotRun() {
    try (ArchitectureContext context = contextFor("empty")) {
      assertThatThrownBy(() -> rule.verify(context))
          .as("an empty selection proves nothing and must not pass silently")
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining("No class declaring TestNG @Test");
    }
  }

  @Test
  public void testClassNamingIsMandatory() {
    assertThat(rule.severity()).isEqualTo(RuleSeverity.ERROR);
  }

  private static ArchitectureContext contextFor(String fixturePackage) {
    return new ArchitectureContext(List.of(FIXTURES + fixturePackage));
  }
}
