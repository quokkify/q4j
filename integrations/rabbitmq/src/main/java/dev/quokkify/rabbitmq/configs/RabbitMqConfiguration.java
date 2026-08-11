package dev.quokkify.rabbitmq.configs;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:local_resources/rabbit.properties",
    "classpath:rabbit.properties"
})
public interface RabbitMqConfiguration extends Config {

  @Config.Key("RABBIT_HOST")
  @Config.DefaultValue("localhost")
  String rabbitHost();

  @Config.Key("RABBIT_PORT")
  @Config.DefaultValue("5672")
  int rabbitPort();

  @Config.Key("RABBIT_VIRTUAL_HOST")
  @Config.DefaultValue("/")
  String rabbitVirtualHost();

  @Config.Key("RABBIT_USER")
  @Config.DefaultValue("guest")
  String rabbitUser();

  @Config.Key("RABBIT_PASSWORD")
  @Config.DefaultValue("guest")
  String rabbitPassword();
}
