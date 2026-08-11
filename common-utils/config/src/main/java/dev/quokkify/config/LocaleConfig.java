package dev.quokkify.config;

import org.aeonbits.owner.Config;

public interface LocaleConfig extends Config {

  @Key("LOCALE")
  @DefaultValue("en")
  String locale();
}
