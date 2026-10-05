package dev.quokkify.architecture.fixtures.naming.violating;

import org.testng.annotations.Test;

/**
 * Fixture for {@code TestClassNamingRuleTest}: a class level {@code @Test} on a class not named {@code *Test}.
 */
@Test(enabled = false)
public class MisnamedClassLevelCase {

  /**
   * Public method the class level annotation would turn into a test.
   */
  public void disabledFixture() {
    // Only the annotation is read; the body never runs.
  }
}
