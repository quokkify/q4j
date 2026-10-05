package dev.quokkify.architecture.fixtures.naming.compliant;

/**
 * Fixture for {@code TestClassNamingRuleTest}: a support class without {@code @Test}, exempt from the suffix.
 */
public class CompliantSteps {

  /**
   * Shared step without a test annotation.
   */
  public void step() {
    // Only the absence of the annotation matters.
  }
}
