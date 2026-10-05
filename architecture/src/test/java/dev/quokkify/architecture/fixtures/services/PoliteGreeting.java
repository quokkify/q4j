package dev.quokkify.architecture.fixtures.services;

/**
 * Fixture for {@code ServiceRegistrationRuleTest}: a provider the {@code ServiceLoader} can create.
 */
public class PoliteGreeting implements Greeting {

  /**
   * Constant inlined by the compiler, visible only through the ClassGraph field model.
   */
  public static final String TEXT = "Good morning";

  @Override
  public String greet() {
    return TEXT;
  }
}
