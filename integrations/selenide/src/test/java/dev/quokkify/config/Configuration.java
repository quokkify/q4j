package dev.quokkify.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:properties", "system:env"})
public interface Configuration extends Config {

  @Key("BASE_URL")
  @DefaultValue("http://localhost")
  String baseUrl();

  @Key("DOWNLOAD_HTTP_BASE_URL")
  @DefaultValue("http://localhost")
  String downloadHttpBaseUrl();

  @Key("DOWNLOAD_BROWSER_BASE_URL")
  @DefaultValue("http://localhost")
  String downloadBrowserBaseUrl();
}
