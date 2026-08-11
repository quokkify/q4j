package dev.quokkify.config.app;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:env", "classpath:inner-tests.properties"})
interface InnerTestsConfiguration extends Config {

  @Key("BASE_API_URL")
  String baseApiUrl();
}
