package dev.quokkify.spi;

import java.util.Locale;
import java.util.ServiceLoader;

public class LocaleProviders {

  private static final LocaleProvider provider = load();

  private static LocaleProvider load() {
    for (LocaleProvider provider : ServiceLoader.load(LocaleProvider.class)) return provider;
    return new DefaultLocaleProvider();
  }

  public static Locale get() {
    return provider.getLocale();
  }
}
