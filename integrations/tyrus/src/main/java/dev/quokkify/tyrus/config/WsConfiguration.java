package dev.quokkify.tyrus.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
  "system:properties",
  "system:env",
  "classpath:local_resources/websockets.properties",
  "classpath:websockets.properties"
})
public interface WsConfiguration extends Config {

  @Config.Key("WS_URL")
  @Config.DefaultValue("ws://localhost:8787/ws")
  String wsUrl();
}
