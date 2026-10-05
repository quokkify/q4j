package dev.quokkify.architecture.contract;

import org.apache.logging.log4j.Level;

/**
 * Severity of an {@link ArchitectureRule}, describing how serious a violation of it is.
 *
 * <p>A severity says nothing about the build outcome on its own. Whether a finding fails the build is
 * decided by comparing it against the gate threshold, which {@code ArchitectureRunner} reads from the
 * {@code architecture.fail.on} property. Keeping the two apart is what lets the same rule set run as a
 * hard gate in one job and as a report in another.
 *
 * <p>The declaration order is significant: it runs from least to most severe, so the natural enum ordering
 * is what the threshold comparison uses. Each constant carries the log level a finding of that severity is
 * reported at.
 *
 * <p>Under the default gate both {@link #WARNING} and {@link #ERROR} fail the build, so a finding that must
 * never block anyone belongs at {@link #INFO}.
 */
public enum RuleSeverity {
  INFO(Level.INFO),
  WARNING(Level.WARN),
  ERROR(Level.ERROR);

  private final Level level;

  RuleSeverity(Level level) {
    this.level = level;
  }

  /**
   * Returns the log level a violation of this severity is reported at.
   *
   * @return matching Log4j level
   */
  public Level getLevel() {
    return level;
  }

  /**
   * Returns the short label used inside the report block, for example {@code WARN} for {@link #WARNING}.
   *
   * @return report label of this severity
   */
  public String getLabel() {
    return level.name();
  }
}
