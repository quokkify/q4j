package dev.quokkify.config;

import org.aeonbits.owner.Config;

/**
 * Interface for config with api settings.
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:env", "system:properties", "classpath:api-config.properties"})
public interface Configuration extends Config {

  @Key("MAX_RESPONSE_TIME_SECONDS")
  @DefaultValue("60")
  Long maxResponseTimeSeconds();
}
