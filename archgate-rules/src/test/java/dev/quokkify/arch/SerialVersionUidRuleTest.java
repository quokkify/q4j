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

public class SerialVersionUidRuleTest {

  private final SerialVersionUidRule rule = new SerialVersionUidRule();

  @Test
  public void fieldsThatAreNotStaticFinalLongAreReported() {
    assertThatThrownBy(() -> rule.verify(FixtureLayout.classes("archfixtures.serial.violating")))
        .isInstanceOf(ArchitectureViolation.class)
        .hasMessageContaining("NonStaticUid.serialVersionUID")
        .hasMessageContaining("NonFinalUid.serialVersionUID")
        .hasMessageContaining("IntUid.serialVersionUID");
  }

  @Test
  public void staticFinalLongPasses() {
    assertThatCode(() -> rule.verify(FixtureLayout.classes("archfixtures.serial.clean")))
        .doesNotThrowAnyException();
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
