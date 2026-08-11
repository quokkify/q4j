package dev.quokkify.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:env", "classpath:browser.properties"})
public interface BrowserConfiguration extends Config {

  @Key("BROWSER")
  @DefaultValue("chrome")
  String browser();

  @Key("BROWSER_SIZE")
  @DefaultValue("1366x768")
  String browserSize();

  @Key("BROWSER_REMOTE_URL")
  String remoteUrl();

  @Key("BROWSER_PROXY_HOST")
  String proxyHost();
}
