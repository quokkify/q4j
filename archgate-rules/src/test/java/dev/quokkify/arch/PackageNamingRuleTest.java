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

public class PackageNamingRuleTest {

  private final PackageNamingRule rule = new PackageNamingRule();

  @Test
  public void packageWithUnderscoreIsReported() {
    assertThatThrownBy(() -> rule.verify(FixtureLayout.classes("archfixtures.packages")))
        .isInstanceOf(ArchitectureViolation.class)
        .hasMessageContaining("Violations (1)")
        .hasMessageContaining("package archfixtures.packages.bad_name")
        .hasMessageNotContaining("archfixtures.packages.good");
  }

  @Test
  public void conventionalPackagePasses() {
    assertThatCode(() -> rule.verify(FixtureLayout.classes("archfixtures.packages.good")))
        .doesNotThrowAnyException();
  }

  @Test
  public void segmentsAreLowerCaseLettersAndDigitsStartingWithALetter() {
    assertThat(PackageNamingRule.isConventional("dev.quokkify.reportportal.e2e")).isTrue();
    assertThat(PackageNamingRule.isConventional("dev.quokkify.bad_name")).isFalse();
    assertThat(PackageNamingRule.isConventional("dev.quokkify.Upper")).isFalse();
    assertThat(PackageNamingRule.isConventional("dev.quokkify.2fa")).isFalse();
    assertThat(PackageNamingRule.isConventional("dev..quokkify")).isFalse();
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
