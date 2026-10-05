package dev.quokkify.architecture.contract;

import java.util.List;

import dev.quokkify.architecture.exceptions.ArchitectureViolationException;

/**
 * A single architecture contract that can be verified against the project.
 *
 * <p>A rule reports a violation by throwing: either an {@link ArchitectureViolationException} with an
 * actionable message, or any other throwable such as the {@link AssertionError} raised by ArchUnit.
 * {@code ArchitectureRunner} catches it and reports it according to {@link #severity()}.
 * A rule that returns normally is considered satisfied.
 *
 * <p>Implementations must be stateless, must declare a public no-argument constructor so the
 * {@link java.util.ServiceLoader} can create them, and must use the shared {@link ArchitectureContext} instead
 * of scanning the classpath themselves.
 */
public interface ArchitectureRule {

  /**
   * Returns the human readable rule name used in the verification report.
   *
   * @return rule name, never {@code null}
   */
  String name();

  /**
   * Returns how serious a violation of this rule is. It does not decide the build outcome on its own:
   * that comparison against the gate threshold is made by {@code ArchitectureRunner}.
   *
   * @return rule severity, never {@code null}
   */
  RuleSeverity severity();

  /**
   * Verifies this contract against the project, throwing when it is violated.
   *
   * @param context shared, already initialised classpath model
   */
  void verify(ArchitectureContext context);

  /**
   * Reports the collected violations as a single failure, or returns when there is none. Every rule ends
   * its verification with this call, so one violation list is always reported as one exception.
   *
   * @param expectedContract description of what the project must satisfy
   * @param violations       one entry per violating class or value, empty when the contract holds
   */
  default void checkViolations(String expectedContract, List<String> violations) {
    if (!violations.isEmpty()) {
      throw new ArchitectureViolationException(expectedContract, violations);
    }
  }
}
