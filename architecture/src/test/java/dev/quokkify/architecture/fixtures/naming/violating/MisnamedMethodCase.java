package dev.quokkify.architecture.fixtures.naming.violating;

import org.testng.annotations.Test;

/**
 * Fixture for {@code TestClassNamingRuleTest}: a method level {@code @Test} in a class not named {@code *Test}.
 */
public class MisnamedMethodCase {

  @Test(enabled = false)
  public void disabledFixture() {
    // Only the annotation is read; the body never runs.
  }
}
