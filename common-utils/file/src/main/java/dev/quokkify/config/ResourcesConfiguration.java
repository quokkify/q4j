package dev.quokkify.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:env", "classpath:resources-config.properties"})
public interface ResourcesConfiguration extends Config {

  @Key("LOCAL_RESOURCES_FOLDER")
  @DefaultValue("local_resources")
  String localResourcesFolder();
}
