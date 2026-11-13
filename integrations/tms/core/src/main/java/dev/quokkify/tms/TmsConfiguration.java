package dev.quokkify.tms;

import org.aeonbits.owner.Config;

/**
 * Settings of the test management system integration.
 *
 * <ul>
 *     <li><b>TMS_REQUIRED</b> - fail the run when no configured TMS provider is on the classpath.</li>
 * </ul>
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:local_resources/tms-config.properties",
    "classpath:tms-config.properties"
})
public interface TmsConfiguration extends Config {

  @Key("TMS_REQUIRED")
  @DefaultValue("false")
  Boolean required();
}
