package dev.quokkify.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:env", "classpath:local_resources/aes-encryption.properties", "classpath:aes-encryption.properties"})
public interface AesEncryptionConfiguration extends Config {

  @Config.Key("ALGORITHM_MODE")
  String algorithmMode();

  @Config.Key("SECRET_KEY")
  String secretKey();

  @Config.Key("INITIALIZATION_VECTOR")
  String initializationVector();
}
