package dev.quokkify.kafka.configs;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:local_resources/kafka.properties",
    "classpath:kafka.properties"
})
public interface KafkaConfiguration extends Config {

  @Config.Key("KAFKA_SERVER_ADDRESS")
  @Config.DefaultValue("localhost:29092")
  String kafkaServerAddress();

  @Config.Key("SECURITY_PROTOCOL")
  @Config.DefaultValue("PLAINTEXT")
  String securityProtocol();

  @Config.Key("SSL_KEYSTORE_PASSWORD")
  String sslKeystorePassword();

  @Config.Key("SSL_KEYSTORE_LOCATION")
  String sslKeystoreLocation();

  @Config.Key("SSL_TRUSTSTORE_PASSWORD")
  String sslTruststorePassword();

  @Config.Key("SSL_TRUSTSTORE_LOCATION")
  String sslTruststoreLocation();
}
