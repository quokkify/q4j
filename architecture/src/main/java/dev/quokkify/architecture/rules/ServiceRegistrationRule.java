package dev.quokkify.architecture.rules;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.ArchitectureRule;
import dev.quokkify.architecture.contract.RuleScope;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import io.github.classgraph.Resource;

/**
 * Verifies that every {@code META-INF/services} registration of the project can be loaded by the
 * {@link java.util.ServiceLoader}.
 *
 * <p>A registration is a plain text file that no compiler checks: a renamed provider, a typo in its name, a
 * provider that no longer implements the service or lost its public no-argument constructor all compile. The
 * mistake surfaces only at runtime, as a {@link java.util.ServiceConfigurationError}, or not at all when the
 * service is optional, and the provider then silently never runs.
 *
 * <p>The files are read from {@link ArchitectureContext#resources()}, so both resource groups must be configured.
 * Service and provider types are resolved against the verification classpath without being initialized, which is
 * why it must hold the runtime classpath of the module, as the {@code ServiceLoader} would at runtime. A
 * registration whose service type cannot be loaded is reported too, although a {@code ServiceLoader} only reads
 * it when that service is requested: a registration for a service that is never on the classpath is dead.
 */
public class ServiceRegistrationRule implements ArchitectureRule {

  private static final String SERVICES_DIRECTORY = "META-INF/services/";
  private static final String COMMENT = "#";
  private static final String EXPECTED_CONTRACT = """
      Every META-INF/services file must be named after a loadable service type and list only public, concrete \
      provider classes that implement it and declare a public no-argument constructor, otherwise the \
      ServiceLoader fails at runtime or the provider never runs. Fix the provider or the registration.""";

  @Override
  public String name() {
    return "Service registrations resolve";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.ERROR;
  }

  @Override
  public Set<RuleScope> scopes() {
    return Set.of(RuleScope.RESOURCES);
  }

  @Override
  public void verify(ArchitectureContext context) {
    List<String> violations = context.resources()
        .filter(resource -> isServiceFile(resource.getPath()))
        .stream()
        .flatMap(registration -> violationsOf(registration).stream())
        .sorted()
        .toList();
    checkViolations(EXPECTED_CONTRACT, violations);
  }

  private static boolean isServiceFile(String path) {
    return path.startsWith(SERVICES_DIRECTORY) && path.length() > SERVICES_DIRECTORY.length()
        && path.indexOf('/', SERVICES_DIRECTORY.length()) < 0;
  }

  private List<String> violationsOf(Resource registration) {
    String location = "%s in %s".formatted(registration.getPath(), registration.getClasspathElementFile());
    String serviceName = registration.getPath().substring(SERVICES_DIRECTORY.length());
    Optional<Class<?>> service = load(serviceName);
    if (service.isEmpty()) {
      return List.of("%s: service type %s cannot be loaded".formatted(location, serviceName));
    }
    List<String> violations = new ArrayList<>();
    for (String providerName : providerNames(registration)) {
      problemOf(service.get(), providerName)
          .ifPresent(problem -> violations.add("%s: provider %s %s".formatted(location, providerName, problem)));
    }
    return violations;
  }

  /**
   * Follows the {@code ServiceLoader} file format: one binary class name per line, {@code #} starts a comment and
   * blank lines are ignored.
   */
  private static List<String> providerNames(Resource registration) {
    try {
      return Arrays.stream(registration.getContentAsString().split("\r\n|\r|\n"))
          .map(line -> line.contains(COMMENT) ? line.substring(0, line.indexOf(COMMENT)) : line)
          .map(String::trim)
          .filter(name -> !name.isEmpty())
          .toList();
    } catch (IOException unreadable) {
      throw new ArchitectureRunnerError("Cannot read the service registration %s, so it cannot be verified."
          .formatted(registration.getPath()), unreadable);
    }
  }

  /**
   * Repeats what the {@code ServiceLoader} requires of a provider on the class path, in the order it checks.
   */
  private Optional<String> problemOf(Class<?> service, String providerName) {
    Optional<Class<?>> loaded = load(providerName);
    if (loaded.isEmpty()) {
      return Optional.of("cannot be loaded");
    }
    Class<?> provider = loaded.get();
    if (!service.isAssignableFrom(provider)) {
      return Optional.of("does not implement " + service.getName());
    }
    int modifiers = provider.getModifiers();
    if (!Modifier.isPublic(modifiers) || Modifier.isAbstract(modifiers) || provider.isInterface()) {
      return Optional.of("is not a public concrete class");
    }
    if (!hasPublicNoArgumentConstructor(provider)) {
      return Optional.of("has no public no-argument constructor");
    }
    return Optional.empty();
  }

  private static boolean hasPublicNoArgumentConstructor(Class<?> provider) {
    try {
      provider.getConstructor();
      return true;
    } catch (NoSuchMethodException missing) {
      return false;
    }
  }

  private Optional<Class<?>> load(String name) {
    try {
      return Optional.of(Class.forName(name, false, getClass().getClassLoader()));
    } catch (ClassNotFoundException | LinkageError unloadable) {
      return Optional.empty();
    }
  }
}
