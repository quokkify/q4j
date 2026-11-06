package dev.quokkify.arch;

import java.util.Set;

import dev.quokkify.archgate.api.ArchitectureViolation;
import dev.quokkify.archgate.api.Scope;
import dev.quokkify.archgate.api.Severity;
import dev.quokkify.archgate.api.UnableToVerifyError;
import dev.quokkify.archgate.context.archunit.ArchUnitContext;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NoInterfaceIPrefixRuleTest {

  private final NoInterfaceIPrefixRule rule = new NoInterfaceIPrefixRule();

  @Test
  public void interfacePrefixedWithIIsReported() {
    assertThatThrownBy(() -> rule.verify(FixtureLayout.classes("archfixtures.iprefix.violating")))
        .isInstanceOf(ArchitectureViolation.class)
        .hasMessageContaining("Violations (1)")
        .hasMessageContaining("archfixtures.iprefix.violating.IClient is an interface prefixed with I");
  }

  @Test
  public void namesStartingWithIAsAWordAndClassesPass() {
    assertThatCode(() -> rule.verify(FixtureLayout.classes("archfixtures.iprefix.clean")))
        .doesNotThrowAnyException();
  }

  @Test
  public void onlyIFollowedByAnUpperCaseLetterIsAPrefix() {
    assertThat(NoInterfaceIPrefixRule.isPrefixedWithI("IClient")).isTrue();
    assertThat(NoInterfaceIPrefixRule.isPrefixedWithI("Item")).isFalse();
    assertThat(NoInterfaceIPrefixRule.isPrefixedWithI("I")).isFalse();
    assertThat(NoInterfaceIPrefixRule.isPrefixedWithI("Client")).isFalse();
  }

  @Test
  public void noClassesToCheckIsUnableToVerify() {
    ArchUnitContext empty = FixtureLayout.classes("archfixtures.missing");
    assertThatThrownBy(() -> rule.verify(empty))
        .as("a rule that checked nothing must not pass")
        .isInstanceOf(UnableToVerifyError.class);
  }

  @Test
  public void ruleIsAnErrorOverMainAndTest() {
    assertThat(rule.severity()).isEqualTo(Severity.ERROR);
    assertThat(rule.scopes()).isEqualTo(Set.of(Scope.MAIN, Scope.TEST));
    assertThat(rule.contextType()).isEqualTo(ArchUnitContext.class);
  }
}
