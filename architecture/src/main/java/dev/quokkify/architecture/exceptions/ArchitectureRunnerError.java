package dev.quokkify.architecture.exceptions;

/**
 * Thrown by a rule that cannot be evaluated at all, as opposed to a contract that is violated.
 *
 * <p>It aborts the whole run, hence the name: a rule that cannot run proves nothing, so this must never be
 * reported under the rule's {@code RuleSeverity} and downgraded to a warning that leaves the build green.
 * It is an {@link Error} on purpose: {@code ArchitectureRunner} only converts {@link AssertionError} and
 * {@link Exception} into severity-scoped findings, so this propagates and fails the build whatever severity
 * the rule declares.
 *
 * <p>Typical causes are a missing classpath element, an empty class selector, or a domain type that no
 * longer has the shape the rule reads.
 */
public class ArchitectureRunnerError extends Error {

  private static final long serialVersionUID = 1L;

  /**
   * Creates an error describing why the rule could not run and how to restore it.
   *
   * @param message actionable description of what is missing or broken
   */
  public ArchitectureRunnerError(String message) {
    super(message);
  }

  /**
   * Creates an error describing why the rule could not run, keeping the underlying cause.
   *
   * @param message actionable description of what is missing or broken
   * @param cause   original failure
   */
  public ArchitectureRunnerError(String message, Throwable cause) {
    super(message, cause);
  }
}
