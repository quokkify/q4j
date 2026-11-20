package dev.quokkify.flutter.comparison;

/**
 * One scenario run in one mode.
 *
 * @param expectedToFail true for the failure demo, which must fail in every mode
 * @param diagnosticsExcerpt first lines of the Flutter suggestions or the native page source, null when not captured
 * @param knownGapReason why the test documents a known gap of its mode, null when it does not
 */
public record ScenarioResult(String scenario, String mode, Status status, long durationMs, long findAttempts,
                             boolean expectedToFail, String diagnosticsExcerpt, String knownGapReason) {

  public ScenarioResult(String scenario, String mode, Status status, long durationMs, long findAttempts,
                        boolean expectedToFail, String diagnosticsExcerpt) {
    this(scenario, mode, status, durationMs, findAttempts, expectedToFail, diagnosticsExcerpt, null);
  }
}
