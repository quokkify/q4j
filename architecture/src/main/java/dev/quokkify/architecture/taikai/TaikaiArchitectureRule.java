package dev.quokkify.architecture.taikai;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.ArchitectureRule;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import com.enofex.taikai.Taikai;

/**
 * Runs a <a href="https://github.com/enofex/taikai">Taikai</a> rule set as one {@link ArchitectureRule}.
 *
 * <p>Taikai ships predefined ArchUnit rules configured through its builder. This adapter evaluates them against
 * the ArchUnit model shared by the run, {@link ArchitectureContext#all()}, so the classpath is imported once
 * however many Taikai rule sets are registered, and their findings land in the same report, under this rule's
 * severity and the same gate threshold as every other rule.
 *
 * <p>Every Taikai rule is evaluated before failing, through {@link Taikai#checkAll()}, so one run reports every
 * violation of the set. Its {@link AssertionError} is reported as this rule's finding.
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

  @Override
  public final void verify(ArchitectureContext context) {
    if (context.all().isEmpty()) {
      // ArchUnit would report an empty import as an ordinary violation; it is an inability to verify instead.
      throw new ArchitectureRunnerError("""
          ArchUnit imported no class from %s, so %s verified nothing. The compiled classes must be on the \
          verification classpath.""".formatted(context.packages(), getClass().getName()));
    }
    Taikai.Builder builder = Taikai.builder()
        .classes(context.all())
        // Taikai writes this flag into the global ArchUnit configuration. True is ArchUnit's own default, so
        // rules evaluated concurrently keep the behaviour they expect, and an empty selection fails loudly.
        .failOnEmpty(true);
    configure(builder);
    Taikai taikai = builder.build();
    if (taikai.rules().isEmpty()) {
      throw new ArchitectureRunnerError(
          "%s configures no Taikai rule, so it verifies nothing.".formatted(getClass().getName()));
    }
    taikai.checkAll();
  }
}
