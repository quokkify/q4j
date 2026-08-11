package dev.quokkify.impl;

import java.util.Locale;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.config.LocaleConfig;
import dev.quokkify.spi.LocaleProvider;

public class LocaleProviderImpl implements LocaleProvider {

  @Override
  public Locale getLocale() {
    String tag = ConfigRegistry.get(LocaleConfig.class).locale();
    return Locale.forLanguageTag(tag);
  }
}
