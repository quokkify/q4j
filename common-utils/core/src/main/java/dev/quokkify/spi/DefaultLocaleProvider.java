package dev.quokkify.spi;

import java.util.Locale;

public class DefaultLocaleProvider implements LocaleProvider {

  @Override
  public Locale getLocale() {
    return Locale.getDefault();
  }
}
