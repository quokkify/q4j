package dev.quokkify.reportportal.configs;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:local_resources/reportportal.properties",
    "classpath:reportportal.properties"
})
public interface ReportPortalConfiguration extends Config {

  @Key("RUN_REPORT_PORTAL")
  @DefaultValue("false")
  Boolean runReportPortal();

  @Key("RP_PROJECT_NAME")
  String rpProjectName();

  @Key("RP_LAUNCH_NAME")
  String rpLaunchName();

  @Key("RP_LAUNCH_MODE")
  String rpLaunchMode();

  @Key("REPORTPORTAL_ENDPOINT")
  String rpEndpoint();

  @Key("REPORTPORTAL_API_KEY")
  String rpApiKey();
}
