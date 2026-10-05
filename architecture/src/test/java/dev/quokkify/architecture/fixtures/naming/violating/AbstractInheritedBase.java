package dev.quokkify.architecture.fixtures.naming.violating;

import org.testng.annotations.Test;

/**
 * Fixture for {@code TestClassNamingRuleTest}: an abstract base whose tests TestNG runs in every subclass.
 */
public abstract class AbstractInheritedBase {

  @Test(enabled = false)
  public void inheritedFixture() {
    // Only the annotation is read; the body never runs.
  }
}
