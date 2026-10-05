package dev.quokkify.architecture.rules;

import java.nio.file.Path;
import java.util.List;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;
import dev.quokkify.architecture.exceptions.ArchitectureViolationException;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ServiceRegistrationRuleTest {

  private static final String FIXTURES = "dev.quokkify.architecture.fixtures.services.";
  private static final Path REGISTRATIONS = Path.of("src/test/resources/services");

  private final ServiceRegistrationRule rule = new ServiceRegistrationRule();

  @Test
  public void loadableRegistrationWithCommentsAndBlankLinesPasses() {
    try (ArchitectureContext context = contextFor(List.of(REGISTRATIONS.resolve("registered")))) {
      assertThatCode(() -> rule.verify(context)).doesNotThrowAnyException();
      assertThat(context.resources().getPaths())
          .as("the registration must be read and accepted, not missed")
          .contains("META-INF/services/" + FIXTURES + "Greeting");
    }
  }

  @Test
  public void everyProviderTheServiceLoaderWouldRejectIsReported() {
    try (ArchitectureContext context = contextFor(List.of(REGISTRATIONS.resolve("misregistered")))) {
      assertThatThrownBy(() -> rule.verify(context))
          .isInstanceOf(ArchitectureViolationException.class)
          .hasMessageContaining("Violations (6)")
          .hasMessageContaining("provider " + FIXTURES + "MissingGreeting cannot be loaded")
          .hasMessageContaining("provider " + FIXTURES + "AbstractGreeting is not a public concrete class")
          .as("the ServiceLoader cannot access a package private provider")
          .hasMessageContaining("provider " + FIXTURES + "HiddenGreeting is not a public concrete class")
          .hasMessageContaining("provider " + FIXTURES + "NamedGreeting has no public no-argument constructor")
          .hasMessageContaining("provider " + FIXTURES + "Farewell does not implement " + FIXTURES + "Greeting")
          .hasMessageContaining("service type " + FIXTURES + "MissingService cannot be loaded")
          .hasMessageNotContaining("provider " + FIXTURES + "PoliteGreeting");
    }
  }

  @Test
  public void projectWithoutResourcesHasNothingToVerify() {
    try (ArchitectureContext context = contextFor(List.of(REGISTRATIONS.resolve("absent")))) {
      assertThatCode(() -> rule.verify(context)).doesNotThrowAnyException();
    }
  }

  @Test
  public void unconfiguredResourcesMeanTheRuleCannotRun() {
    try (ArchitectureContext context = new ArchitectureContext(List.of(FIXTURES + "unused"))) {
      assertThatThrownBy(() -> rule.verify(context))
          .as("a rule that read no resource proves nothing and must not pass silently")
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining(ArchitectureContext.MAIN_RESOURCES_PROPERTY);
    }
  }

  @Test
  public void brokenRegistrationIsAnError() {
    assertThat(rule.severity()).isEqualTo(RuleSeverity.ERROR);
  }

  private static ArchitectureContext contextFor(List<Path> testResources) {
    return ArchitectureContext.builder(List.of(FIXTURES + "unused"))
        .mainResources(List.of())
        .testResources(testResources)
        .build();
  }
}
