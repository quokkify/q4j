package dev.quokkify.architecture.exceptions;

import java.util.Collection;

/**
 * Thrown by a rule when an architecture contract is violated.
 *
 * <p>The message is part of the build output, so it must name the violating class or value and the
 * expected contract.
 */
public class ArchitectureViolationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates an exception with an already formatted message.
   *
   * @param message actionable description of the violation
   */
  public ArchitectureViolationException(String message) {
    super(message);
  }

  /**
   * Creates an exception listing every violation followed by the expected contract.
   *
   * @param expectedContract description of what the project must satisfy
   * @param violations       one entry per violating class or value
   */
  public ArchitectureViolationException(String expectedContract, Collection<String> violations) {
    super(format(expectedContract, violations));
  }

  private static String format(String expectedContract, Collection<String> violations) {
    StringBuilder message = new StringBuilder(expectedContract.length() + violations.size() * 64);
    message.append(expectedContract)
        .append(System.lineSeparator())
        .append("Violations (")
        .append(violations.size())
        .append("):");
    violations.forEach(violation -> message.append(System.lineSeparator())
        .append("    - ")
        .append(violation));
    return message.toString();
  }
}
