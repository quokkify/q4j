package dev.quokkify.architecture.taikai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.ArchitectureRule;
import dev.quokkify.architecture.contract.ClassScope;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

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
 * {@link AssertionError} reported as this rule's finding. A single Taikai rule that matches no class holds;
 * a scope that imports no class at all aborts the run instead.
 *
 * <p>Subclasses only declare {@link #name()}, {@link #severity()} and {@link #configure(Taikai.Builder)}, and
 * need a public no-argument constructor to be discovered by the {@link java.util.ServiceLoader}.
 */
public abstract class TaikaiArchitectureRule implements ArchitectureRule {

  private static final String FAIL_ON_EMPTY_SHOULD = "archRule.failOnEmptyShould";

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
    Taikai.Builder builder = Taikai.builder()
        .classes(classes)
        // Taikai writes this flag into the global ArchUnit configuration on build(). Passing the current value
        // makes that write a no-op, so rules evaluated concurrently, and the consumer's archunit.properties, keep
        // the behaviour they configured. The adapter does not rely on the flag: see evaluate().
        .failOnEmpty(globalFailOnEmpty());
    configure(builder);
    Taikai taikai = builder.build();
    if (taikai.rules().isEmpty()) {
      throw new ArchitectureRunnerError(
          "%s configures no Taikai rule, so it verifies nothing.".formatted(getClass().getName()));
    }
    evaluate(taikai, classes);
  }

  /**
   * Evaluates every rule like {@link Taikai#checkAll()}, but lets an individual rule match nothing: a convention
   * about, say, {@code serialVersionUID} holds trivially in a module without serializable classes. An empty
   * import of the whole scope is still rejected by {@link #verify(ArchitectureContext)}.
   */
  private static void evaluate(Taikai taikai, JavaClasses classes) {
    List<String> failures = new ArrayList<>();
    int violations = 0;
    for (TaikaiRule rule : taikai.rules()) {
      FailureReport report = rule.archRule()
          .allowEmptyShould(true)
          .evaluate(classesOf(rule, classes))
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
   * Honours the classes and exclusions a Taikai configurer attached to one rule, on top of the scope.
   */
  private static JavaClasses classesOf(TaikaiRule rule, JavaClasses classes) {
    TaikaiRule.Configuration configuration = rule.configuration();
    JavaClasses selected = Objects.nonNull(configuration.javaClasses()) ? configuration.javaClasses() : classes;
    Collection<String> excluded = configuration.excludedClasses();
    if (excluded.isEmpty()) {
      return selected;
    }
    return selected.that(DescribedPredicate.describe("not excluded by Taikai",
        javaClass -> !excluded.contains(javaClass.getName())));
  }

  private static boolean globalFailOnEmpty() {
    return Boolean.parseBoolean(ArchConfiguration.get().getPropertyOrDefault(FAIL_ON_EMPTY_SHOULD, "true"));
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
