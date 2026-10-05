package dev.quokkify.architecture.fixtures.services;

/**
 * Fixture for {@code ServiceRegistrationRuleTest}: a package private provider the {@code ServiceLoader} cannot
 * access.
 */
class HiddenGreeting implements Greeting {

  @Override
  public String greet() {
    return "psst";
  }
}
