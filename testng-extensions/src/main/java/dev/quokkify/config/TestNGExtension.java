package dev.quokkify.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Mutable;
import org.aeonbits.owner.Reloadable;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:env", "classpath:testng.properties"})
public interface TestNGExtension extends Config, Reloadable, Mutable {

  @Key("RETRY_COUNT")
  @DefaultValue("2")
  Integer retryCount();

  @Key("TEST_THREAD_COUNT")
  @DefaultValue("5")
  Integer testThreadCount();

  @Key("TEST_GROUP")
  String testGroup();

  @Key("SUITE_NAME")
  @DefaultValue("Default suite")
  String suiteName();

  @Key("EXECUTION_MODE")
  @DefaultValue("LOCAL")
  ExecutionMode mode();

  enum ExecutionMode {
    LOCAL, CI, DIND
  }
}
