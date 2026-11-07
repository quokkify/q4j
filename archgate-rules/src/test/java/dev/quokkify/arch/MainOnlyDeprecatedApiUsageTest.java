package dev.quokkify.arch;

import dev.quokkify.archgate.api.ArchitectureViolation;
import dev.quokkify.archgate.api.UnableToVerifyError;
import dev.quokkify.archgate.context.archunit.ArchUnitContext;
import dev.quokkify.archgate.rules.archunit.NoDeprecatedApiUsageRule;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression guard for tools/archgate-main: ArchGate's {@link NoDeprecatedApiUsageRule} must check modules that have
 * main classes but no tests, which ArchGate 0.1.x could not.
 */
public class MainOnlyDeprecatedApiUsageTest {

  private final NoDeprecatedApiUsageRule rule = new NoDeprecatedApiUsageRule();

  @Test
  public void deprecatedCallInMainClassesIsReportedWithoutTests() {
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
  public void noClassesAtAllIsUnableToVerify() {
    ArchUnitContext empty = FixtureLayout.mainClasses("archfixtures.missing");
    assertThatThrownBy(() -> rule.verify(empty))
        .as("a rule that checked nothing must not pass")
        .isInstanceOf(UnableToVerifyError.class);
  }
}
