package dev.quokkify.architecture.rules;

import java.util.stream.Stream;

import dev.quokkify.architecture.contract.ClassScope;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.taikai.TaikaiArchitectureRule;

import com.enofex.taikai.Taikai;
import com.enofex.taikai.TaikaiRule;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * Verifies that main code never writes to the console directly.
 *
 * <p>Library code that prints through {@code System.out}, {@code System.err}, {@code printStackTrace()} or
 * {@code Thread.dumpStack()} bypasses the logging configuration of the consumer: the output cannot be filtered,
 * routed or silenced, and a stack trace printed next to a log call is reported twice. Main code must log through
 * a logger instead.
 *
 * <p>The check reads bytecode, so a statically imported {@code out}, {@code System.out::println} and a stream
 * passed on as an argument are all seen as the field access they compile to, and a method reference such as
 * {@code Throwable::printStackTrace} is inspected like a call. {@code System.out} and {@code System.err} come
 * from Taikai's {@code noUsageOfSystemOutOrErr}; {@code printStackTrace()} without arguments and
 * {@code Thread.dumpStack()} are added as one ArchUnit rule. {@code printStackTrace(writer)} is allowed: it
 * renders into a target the caller chose.
 *
 * <p>The module's runtime classpath must be available so that the owner of a call, say {@code JSchException},
 * resolves to a {@code Throwable}; an owner that does not resolve is reported rather than trusted.
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
        .addRule(TaikaiRule.of(ArchRuleDefinition.classes()
            .should(notPrintStackTraces())
            .because("a stack trace printed to the console bypasses the logging configuration")));
  }

  /**
   * Inspects calls and method references alike: {@code failures.forEach(Throwable::printStackTrace)} compiles to
   * a method reference, not to a call.
   */
  private static ArchCondition<JavaClass> notPrintStackTraces() {
    return new ArchCondition<>("not call printStackTrace() or Thread.dumpStack()") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        Stream.concat(javaClass.getMethodCallsFromSelf().stream(), javaClass.getMethodReferencesFromSelf().stream())
            .filter(NoConsoleOutputRule::printsStackTrace)
            .forEach(access -> events.add(SimpleConditionEvent.violated(access, access.getDescription())));
      }
    };
  }

  private static boolean printsStackTrace(JavaCodeUnitAccess<?> access) {
    JavaClass owner = access.getTargetOwner();
    boolean bareStackTrace = PRINT_STACK_TRACE.equals(access.getName())
        && access.getTarget().getRawParameterTypes().isEmpty()
        && (owner.isAssignableTo(Throwable.class) || isUnresolved(owner));
    boolean dumpStack = DUMP_STACK.equals(access.getName()) && owner.isEquivalentTo(Thread.class);
    return bareStackTrace || dumpStack;
  }

  /**
   * An owner whose hierarchy could not be resolved from the classpath cannot be proven not to be a
   * {@code Throwable}, so a zero argument {@code printStackTrace()} on it is reported rather than let through.
   */
  private static boolean isUnresolved(JavaClass owner) {
    return owner.getRawSuperclass().isEmpty() && !owner.isInterface() && !owner.isEquivalentTo(Object.class);
  }
}
