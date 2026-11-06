package dev.quokkify.arch;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import dev.quokkify.archgate.api.ArchitectureRule;
import dev.quokkify.archgate.api.Scope;
import dev.quokkify.archgate.api.Severity;
import dev.quokkify.archgate.context.archunit.ArchUnitContext;
import dev.quokkify.archgate.context.archunit.support.ArchRuleChecks;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * Verifies that every {@code serialVersionUID} field is {@code static final long}, in main and test code.
 *
 * <p>Java serialization reads the field only when it has exactly this shape; any other declaration is silently
 * ignored and the class falls back to a computed version that changes with every edit.
 */
public class SerialVersionUidRule implements ArchitectureRule<ArchUnitContext> {

  private static final String FIELD_NAME = "serialVersionUID";
  private static final String EXPECTED_CONTRACT = """
      A serialVersionUID field must be declared static final long, otherwise serialization ignores it and \
      computes a version that changes with every edit of the class.""";

  @Override
  public String name() {
    return "serialVersionUID is static final long";
  }

  @Override
  public Severity severity() {
    return Severity.ERROR;
  }

  @Override
  public Set<Scope> scopes() {
    return EnumSet.of(Scope.MAIN, Scope.TEST);
  }

  @Override
  public Class<ArchUnitContext> contextType() {
    return ArchUnitContext.class;
  }

  @Override
  public void verify(ArchUnitContext context) {
    List<String> violations = ArchRuleChecks.violations(definition(), context.all());
    checkViolations(EXPECTED_CONTRACT, violations);
  }

  static ArchRule definition() {
    return ArchRuleDefinition.fields()
        .that().haveName(FIELD_NAME)
        .should().beStatic()
        .andShould().beFinal()
        .andShould().haveRawType(long.class)
        .allowEmptyShould(true)
        .because("serialization only reads a static final long serialVersionUID");
  }
}
