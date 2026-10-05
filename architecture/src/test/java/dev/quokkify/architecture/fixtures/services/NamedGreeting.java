package dev.quokkify.architecture.fixtures.services;

/**
 * Fixture for {@code ServiceRegistrationRuleTest}: a provider without a no-argument constructor.
 */
public class NamedGreeting implements Greeting {

  private final String name;

  /**
   * Creates a greeting for one person.
   *
   * @param name person to greet
   */
  public NamedGreeting(String name) {
    this.name = name;
  }

  @Override
  public String greet() {
    return "Hello " + name;
  }
}
