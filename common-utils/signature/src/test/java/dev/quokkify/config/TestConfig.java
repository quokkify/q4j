package dev.quokkify.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:env", "classpath:test.properties"})
@Config.DecryptorClass(ConfigAesDecryptor.class)
public interface TestConfig extends Config {

  @EncryptedValue
  @Key("TEST_DECRYPTED_STRING")
  String testDecryptedString();
}
