package dev.quokkify.arch;

import java.util.Set;

import dev.quokkify.archgate.api.ArchitectureViolation;
import dev.quokkify.archgate.api.Scope;
import dev.quokkify.archgate.api.Severity;
import dev.quokkify.archgate.api.UnableToVerifyError;
import dev.quokkify.archgate.context.archunit.ArchUnitContext;
import dev.quokkify.archgate.rules.archunit.NoDeprecatedApiUsageRule;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NoDeprecatedApiUsageMainRuleTest {

  private final NoDeprecatedApiUsageMainRule rule = new NoDeprecatedApiUsageMainRule();

  @Test
  public void deprecatedCallInMainClassesIsReportedWithoutATestView() {
    ArchUnitContext mainOnly = FixtureLayout.mainClasses("archfixtures.deprecated.violating");
    assertThatThrownBy(() -> rule.verify(mainOnly))
        .isInstanceOf(ArchitectureViolation.class)
        .hasMessageContaining("Violations (1)")
        .hasMessageContaining("UsesLegacyApi.value()")
        .hasMessageContaining("LegacyApi.oldValue()");
  }

  @Test
  public void mainClassesWithoutDeprecatedUsagePass() {
    assertThatCode(() -> rule.verify(FixtureLayout.mainClasses("archfixtures.deprecated.clean")))
        .doesNotThrowAnyException();
  }

  @Test
  public void upstreamRuleCannotRunWithoutATestView() {
    ArchUnitContext mainOnly = FixtureLayout.mainClasses("archfixtures.deprecated.clean");
    assertThatThrownBy(() -> new NoDeprecatedApiUsageRule().verify(mainOnly))
        .as("the reason this rule exists: modules without tests have no test view")
        .isInstanceOf(UnableToVerifyError.class);
  }

  @Test
  public void noMainClassesIsUnableToVerify() {
    ArchUnitContext empty = FixtureLayout.mainClasses("archfixtures.missing");
    assertThatThrownBy(() -> rule.verify(empty))
        .as("a rule that checked nothing must not pass")
        .isInstanceOf(UnableToVerifyError.class);
  }

  @Test
  public void ruleKeepsTheUpstreamSeverityOverMainOnly() {
    assertThat(rule.severity()).isEqualTo(Severity.WARNING).isEqualTo(new NoDeprecatedApiUsageRule().severity());
    assertThat(rule.scopes()).isEqualTo(Set.of(Scope.MAIN));
    assertThat(rule.name()).isEqualTo("No deprecated API usage (main)");
  }
}
