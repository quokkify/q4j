package dev.quokkify.architecture.fixtures.naming.compliant;

import org.testng.annotations.Test;

/**
 * Fixture for {@code TestClassNamingRuleTest}: a correctly named test class. Disabled, so it never runs.
 */
public class CompliantTest {

  @Test(enabled = false)
  public void disabledFixture() {
    // Only the annotation is read; the body never runs.
  }
}
