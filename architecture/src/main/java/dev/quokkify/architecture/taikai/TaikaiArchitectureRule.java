package dev.quokkify.architecture.taikai;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.ArchitectureRule;
import dev.quokkify.architecture.contract.ClassScope;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import com.enofex.taikai.Namespace;
import com.enofex.taikai.Taikai;
import com.enofex.taikai.TaikaiRule;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.FailureReport;

/**
 * Runs a <a href="https://github.com/enofex/taikai">Taikai</a> rule set as one {@link ArchitectureRule}.
 *
 * <p>Taikai ships predefined ArchUnit rules configured through its builder. This adapter evaluates them against
 * the ArchUnit model shared by the run, restricted to {@link #scope()}, so the classes are imported once however
 * many Taikai rule sets are registered, and their findings land in the same report, under this rule's severity
 * and the same gate threshold as every other rule. Taikai's own namespace import options are not used: the
 * scope decides between main and test classes.
 *
 * <p>Every Taikai rule is evaluated before failing, so one run reports every violation of the set, as one
 * {@link AssertionError} reported as this rule's finding. A single Taikai rule that matches no class holds
 * unless {@link #allowsRulesMatchingNothing()} says otherwise; a scope that imports no class at all aborts the
 * run instead, and so does a Taikai rule that asks for classes outside the scope.
 *
 * <p>Subclasses only declare {@link #name()}, {@link #severity()} and {@link #configure(Taikai.Builder)}, and
 * need a public no-argument constructor to be discovered by the {@link java.util.ServiceLoader}.
 */
public abstract class TaikaiArchitectureRule implements ArchitectureRule {

  /**
   * Adds the Taikai rules of this set to the builder, for example
   * {@code builder.java(java -> java.noUsageOfDeprecatedAPIs())}.
   *
   * <p>The classes to verify are supplied by the adapter: a subclass must not set a namespace or classes.
   *
   * @param builder Taikai builder already bound to the classes of the run
   */
  protected abstract void configure(Taikai.Builder builder);

  /**
   * Returns which classes this rule set verifies. Defaults to {@link ClassScope#MAIN}, Taikai's own default.
   *
   * @return scope of the verified classes
   */
  protected ClassScope scope() {
    return ClassScope.MAIN;
  }

  /**
   * Tells whether a single Taikai rule may match no class and still hold. Defaults to {@code true}: a convention
   * about, say, {@code serialVersionUID} holds trivially in a module without serializable classes.
   *
   * <p>The price is that a selector which never matches, a mistyped logger type for instance, passes forever.
   * Return {@code false} for a rule set whose every rule must find something in each verified project.
   *
   * @return whether a Taikai rule matching nothing holds
   */
  protected boolean allowsRulesMatchingNothing() {
    return true;
  }

  @Override
  public final void verify(ArchitectureContext context) {
    JavaClasses classes = scope().classesOf(context);
    if (classes.isEmpty()) {
      if (declaresNoClasses(context)) {
        return;
      }
      // ArchUnit would report an empty import as an ordinary violation; it is an inability to verify instead.
      throw new ArchitectureRunnerError("""
          ArchUnit imported no %s class from %s, so %s verified nothing. The compiled classes must be on the \
          verification classpath.""".formatted(scope(), context.packages(), getClass().getName()));
    }
    Taikai taikai = build(classes);
    if (taikai.rules().isEmpty()) {
      throw new ArchitectureRunnerError(
          "%s configures no Taikai rule, so it verifies nothing.".formatted(getClass().getName()));
    }
    if (Objects.nonNull(taikai.namespace())) {
      throw new ArchitectureRunnerError("""
          %s sets a Taikai namespace, which this adapter ignores: the classes come from the run's scope. Remove \
          the namespace and override scope() instead.""".formatted(getClass().getName()));
    }
    evaluate(taikai, context, classes);
  }

  /**
   * {@code Taikai}'s constructor writes {@code archRule.failOnEmptyShould} into the ArchUnit configuration. Building
   * it in a thread local scope keeps that write away from the global configuration, which rules evaluated
   * concurrently and the consumer's {@code archunit.properties} rely on.
   */
  private Taikai build(JavaClasses classes) {
    return ArchConfiguration.withThreadLocalScope(configuration -> {
      Taikai.Builder builder = Taikai.builder().classes(classes);
      configure(builder);
      return builder.build();
    });
  }

  /**
   * Evaluates every rule like {@link Taikai#checkAll()}, collecting all violations before failing. An empty
   * import of the whole scope is rejected by {@link #verify(ArchitectureContext)} before this runs.
   */
  private void evaluate(Taikai taikai, ArchitectureContext context, JavaClasses classes) {
    List<String> failures = new ArrayList<>();
    int violations = 0;
    for (TaikaiRule rule : taikai.rules()) {
      FailureReport report = rule.archRule()
          .allowEmptyShould(allowsRulesMatchingNothing())
          .evaluate(classesOf(rule, taikai, context, classes))
          .getFailureReport();
      if (!report.isEmpty()) {
        violations += report.getDetails().size();
        failures.add("Rule: " + rule.archRule().getDescription());
        report.getDetails().forEach(detail -> failures.add("\t" + detail));
      }
    }
    if (!failures.isEmpty()) {
      throw new AssertionError("Found %d Taikai violation(s):%n%s"
          .formatted(violations, String.join(System.lineSeparator(), failures)));
    }
  }

  /**
   * Honours what a Taikai configurer attached to one rule, on top of the scope: its own classes, its import
   * option and the exclusions of the rule and of the whole set. A rule that asks for classes outside the scope
   * cannot be verified, so it aborts instead of silently matching nothing.
   */
  private JavaClasses classesOf(TaikaiRule rule, Taikai taikai, ArchitectureContext context, JavaClasses classes) {
    TaikaiRule.Configuration configuration = rule.configuration();
    JavaClasses bound = configuration.javaClasses();
    JavaClasses selected = Objects.nonNull(bound)
        ? requireNotEmpty(bound, rule)
        : classesForImport(configuration.namespaceImport(), rule, context, classes);
    List<String> excluded = new ArrayList<>(configuration.excludedClasses());
    excluded.addAll(taikai.excludedClasses());
    if (excluded.isEmpty()) {
      return selected;
    }
    return selected.that(DescribedPredicate.describe("not excluded by Taikai",
        javaClass -> !excluded.contains(javaClass.getName())));
  }

  private JavaClasses classesForImport(Namespace.IMPORT namespaceImport, TaikaiRule rule, ArchitectureContext context,
      JavaClasses classes) {
    ClassScope scope = scope();
    if (namespaceImport == Namespace.IMPORT.ONLY_TESTS) {
      if (scope == ClassScope.MAIN) {
        throw outsideScope(rule, namespaceImport);
      }
      return scope == ClassScope.ALL ? context.testClasses() : classes;
    }
    if (namespaceImport == Namespace.IMPORT.WITH_TESTS && scope != ClassScope.ALL) {
      throw outsideScope(rule, namespaceImport);
    }
    return classes;
  }

  private ArchitectureRunnerError outsideScope(TaikaiRule rule, Namespace.IMPORT namespaceImport) {
    return new ArchitectureRunnerError("""
        Taikai rule '%s' imports %s, which scope %s of %s does not contain, so it would verify nothing. Override \
        scope().""".formatted(rule.archRule().getDescription(), namespaceImport, scope(), getClass().getName()));
  }

  private static JavaClasses requireNotEmpty(JavaClasses classes, TaikaiRule rule) {
    if (classes.isEmpty()) {
      throw new ArchitectureRunnerError("Taikai rule '%s' is bound to an empty set of classes, so it verifies nothing."
          .formatted(rule.archRule().getDescription()));
    }
    return classes;
  }

  /**
   * A scope configured with no class directory states that the project has no such classes, which is valid.
   * Without configured directories the classes come from the classpath and an empty import is a broken setup.
   */
  private boolean declaresNoClasses(ArchitectureContext context) {
    try {
      return scope().dirsOf(context).isEmpty();
    } catch (ArchitectureRunnerError notConfigured) {
      return false;
    }
  }
}
