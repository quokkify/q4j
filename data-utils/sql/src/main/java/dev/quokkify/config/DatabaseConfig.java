package dev.quokkify.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:env"})
public interface DatabaseConfig extends Config {

  @Key("SQL_DATABASE_DRIVER")
  @DefaultValue("org.postgresql.Driver")
  String driver();

  @Key("SQL_DATABASE_URL")
  String url();

  @Key("SQL_DATABASE_USER")
  String user();

  @Key("SQL_DATABASE_PASSWORD")
  String password();

  @Key("SQL_DATABASE_POOL_SIZE")
  @DefaultValue("100")
  String poolSize();
}
