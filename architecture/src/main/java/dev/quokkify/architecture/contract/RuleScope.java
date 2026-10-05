package dev.quokkify.architecture.contract;

/**
 * Part of the project an {@link ArchitectureRule} verifies, shown next to the rule in the report.
 *
 * <p>A scope names what the rule checks, not which model it reads: a rule that scans every class through
 * {@link ArchitectureContext#scan()} but only judges test classes declares {@link #TEST}.
 */
public enum RuleScope {

  /**
   * Main classes or main sources.
   */
  MAIN,

  /**
   * Test classes or test sources.
   */
  TEST,

  /**
   * Resource files, such as {@code META-INF/services} registrations, of main and test alike.
   */
  RESOURCES
}
