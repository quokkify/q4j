package dev.quokkify.reportportal.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:local_resources/reportportal-test.properties",
    "classpath:reportportal-test.properties"
})
public interface ReportPortalConnectionConfiguration extends Config {

  @Key("REPORTPORTAL_ENDPOINT")
  String endpoint();

  @Key("REPORTPORTAL_API_KEY")
  String apiKey();

  @Key("REPORTPORTAL_PROJECT_NAME")
  @DefaultValue("quokkify")
  String projectName();
}
