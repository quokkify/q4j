package dev.quokkify.reportportal.config;

import dev.quokkify.config.ConfigRegistry;

public class ReportPortalConnectionConfig {

  private static final ReportPortalConnectionConfiguration CONFIG =
      ConfigRegistry.get(ReportPortalConnectionConfiguration.class);

  public static final String ENDPOINT = CONFIG.endpoint();
  public static final String API_KEY = CONFIG.apiKey();
  public static final String PROJECT_NAME = CONFIG.projectName();

  private ReportPortalConnectionConfig() {
  }
}
