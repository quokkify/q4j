package dev.quokkify.arch;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import dev.quokkify.archgate.api.ArchitectureRule;
import dev.quokkify.archgate.api.Scope;
import dev.quokkify.archgate.api.Severity;
import dev.quokkify.archgate.context.archunit.ArchUnitContext;
import dev.quokkify.archgate.context.archunit.support.ArchRuleChecks;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * Verifies that no interface name starts with {@code I} followed by an upper-case letter, such as {@code IClient},
 * in main and test code. Names such as {@code Iterable} or {@code Item} are allowed.
 */
public class NoInterfaceIPrefixRule implements ArchitectureRule<ArchUnitContext> {

  private static final String PREFIX = "I";
  private static final String EXPECTED_CONTRACT = """
      Interfaces are named after what they are, without a Hungarian I prefix: name the interface Client, not \
      IClient, and give the implementation the more specific name.""";

  @Override
  public String name() {
    return "Interfaces have no I prefix";
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
    return ArchRuleDefinition.classes()
        .that().areInterfaces()
        .should(notBePrefixedWithI())
        .allowEmptyShould(true);
  }

  static boolean isPrefixedWithI(String simpleName) {
    return simpleName.length() > PREFIX.length()
        && simpleName.startsWith(PREFIX)
        && Character.isUpperCase(simpleName.charAt(PREFIX.length()));
  }

  private static ArchCondition<JavaClass> notBePrefixedWithI() {
    return new ArchCondition<>("not be prefixed with I") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        if (isPrefixedWithI(javaClass.getSimpleName())) {
          events.add(SimpleConditionEvent.violated(javaClass,
              "%s is an interface prefixed with I".formatted(javaClass.getName())));
        }
      }
    };
  }
}
