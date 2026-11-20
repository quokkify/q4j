package dev.quokkify.flutter.comparison;

/**
 * Outcome of one scenario in one mode. {@code NOT_REACHABLE} is a scenario the mode cannot express; {@code KNOWN_GAP}
 * is a documented failure of the mode in a test that names the gap.
 */
public enum Status {
  PASSED,
  FAILED,
  NOT_REACHABLE,
  KNOWN_GAP
}
