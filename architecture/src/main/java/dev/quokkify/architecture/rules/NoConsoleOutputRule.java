package dev.quokkify.architecture.rules;

import dev.quokkify.architecture.contract.ClassScope;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.taikai.TaikaiArchitectureRule;

import com.enofex.taikai.Taikai;
import com.enofex.taikai.TaikaiRule;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * Verifies that main code never writes to the console directly.
 *
 * <p>Library code that prints through {@code System.out}, {@code System.err}, {@code printStackTrace()} or
 * {@code Thread.dumpStack()} bypasses the logging configuration of the consumer: the output cannot be filtered,
 * routed or silenced, and a stack trace printed next to a log call is reported twice. Main code must log through
 * a logger instead.
 *
 * <p>The check reads bytecode, so a statically imported {@code out}, a method reference such as
 * {@code System.out::println} and a stream passed on as an argument are all seen as the field access they
 * compile to. {@code System.out} and {@code System.err} come from Taikai's {@code noUsageOfSystemOutOrErr};
 * {@code printStackTrace()} without arguments and {@code Thread.dumpStack()} are added as one ArchUnit rule.
 * {@code printStackTrace(writer)} is allowed: it renders into a target the caller chose.
 */
public class NoConsoleOutputRule extends TaikaiArchitectureRule {

  private static final String PRINT_STACK_TRACE = "printStackTrace";
  private static final String DUMP_STACK = "dumpStack";

  @Override
  public String name() {
    return "No console output in main code";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.ERROR;
  }

  @Override
  protected ClassScope scope() {
    return ClassScope.MAIN;
  }

  @Override
  protected void configure(Taikai.Builder builder) {
    builder
        .java(java -> java.noUsageOfSystemOutOrErr())
        .addRule(TaikaiRule.of(ArchRuleDefinition.noClasses()
            .should().callMethodWhere(printsStackTrace())
            .because("a stack trace printed to the console bypasses the logging configuration")));
  }

  private static DescribedPredicate<JavaCall<?>> printsStackTrace() {
    return DescribedPredicate.describe("printStackTrace() or Thread.dumpStack()", call -> {
      boolean bareStackTrace = PRINT_STACK_TRACE.equals(call.getName())
          && call.getTarget().getRawParameterTypes().isEmpty()
          && call.getTargetOwner().isAssignableTo(Throwable.class);
      boolean dumpStack = DUMP_STACK.equals(call.getName())
          && call.getTargetOwner().isEquivalentTo(Thread.class);
      return bareStackTrace || dumpStack;
    });
  }
}
