package dev.quokkify.flutter.device;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * How a device scenario drives the app: through the Flutter integration driver or through native UiAutomator2.
 */
public enum MobileMode {
  FLUTTER,
  NATIVE;

  static final String PROPERTY = "mobile.modes";
  private static final String DEFAULT_MODES = "flutter";

  /**
   * Reads the modes to run from the comma separated system property {@code mobile.modes} (default {@code flutter}).
   */
  public static List<MobileMode> fromProperty() {
    String value = System.getProperty(PROPERTY, DEFAULT_MODES);
    List<MobileMode> modes = Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(mode -> !mode.isEmpty())
        .map(MobileMode::parse)
        .distinct()
        .toList();
    if (modes.isEmpty()) {
      throw new IllegalArgumentException(PROPERTY + " names no mode: '" + value + "'");
    }
    return modes;
  }

  public String label() {
    return name().toLowerCase(Locale.ROOT);
  }

  private static MobileMode parse(String mode) {
    try {
      return valueOf(mode.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(PROPERTY + " has an unknown mode '" + mode + "'; expected flutter or native",
          e);
    }
  }
}
