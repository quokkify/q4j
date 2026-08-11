package dev.quokkify.config.app;

import dev.quokkify.config.ConfigRegistry;

public class InnerTestsConfig {

  private static final InnerTestsConfiguration CONFIG = ConfigRegistry.get(InnerTestsConfiguration.class);

  public static final String BASE_API_URL = CONFIG.baseApiUrl();

  private InnerTestsConfig() {
  }
}
