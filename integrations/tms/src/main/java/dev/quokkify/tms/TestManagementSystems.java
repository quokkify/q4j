package dev.quokkify.tms;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.quokkify.config.ConfigRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Resolves the single test management system provider on the classpath.
 */
public final class TestManagementSystems {

  private static final Logger LOG = LogManager.getLogger(TestManagementSystems.class);

  private TestManagementSystems() {
  }

  /**
   * @return the provider found by {@link ServiceLoader} if it is the only one and is configured
   * @throws TmsConfigurationException if more than one provider is on the classpath, or if {@code TMS_REQUIRED}
   *                                   is {@code true} and no configured provider is available
   */
  public static Optional<TestManagementSystem> resolve() {
    List<TestManagementSystem> providers =
        StreamSupport.stream(ServiceLoader.load(TestManagementSystem.class).spliterator(), false).toList();
    return resolve(providers, ConfigRegistry.get(TmsConfiguration.class).required());
  }

  static Optional<TestManagementSystem> resolve(List<TestManagementSystem> providers, boolean required) {
    if (providers.size() > 1) {
      throw new TmsConfigurationException(
          "Expected exactly one TMS provider on the classpath, found %d: %s".formatted(
              providers.size(),
              providers.stream()
                  .map(provider -> provider.getClass().getName())
                  .collect(Collectors.joining(", "))));
    }
    if (providers.isEmpty()) {
      return unavailable("No TMS provider is on the classpath, results are not reported", required);
    }
    TestManagementSystem provider = providers.get(0);
    if (!provider.isConfigured()) {
      return unavailable("TMS provider '%s' is not configured, results are not reported".formatted(provider.id()),
          required);
    }
    return Optional.of(provider);
  }

  private static Optional<TestManagementSystem> unavailable(String message, boolean required) {
    if (required) {
      throw new TmsConfigurationException(message + " (TMS_REQUIRED=true)");
    }
    LOG.warn(message);
    return Optional.empty();
  }
}
