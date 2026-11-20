package dev.quokkify.flutter.device;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * How a device scenario drives the app: through the Flutter integration driver, either polled by Selenide
 * ({@code flutter}) or waiting on the server ({@code flutter-sync}), or through native UiAutomator2.
 */
public enum MobileMode {
  FLUTTER("flutter"),
  FLUTTER_SYNC("flutter-sync"),
  NATIVE("native");

  static final String PROPERTY = "mobile.modes";
  private static final String DEFAULT_MODES = "flutter";

  private final String label;

  MobileMode(String label) {
    this.label = label;
  }

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

  /**
   * The value of this mode in {@code mobile.modes}, in reports and in the {@code not reachable in <label> mode:}
   * message.
   */
  public String label() {
    return label;
  }

  /**
   * The mode whose locators, screens and known gaps this mode shares: {@code flutter-sync} drives the same apk through
   * the same driver as {@code flutter} and differs only in where the find waits.
   */
  public MobileMode family() {
    return this == FLUTTER_SYNC ? FLUTTER : this;
  }

  public boolean isFlutter() {
    return family() == FLUTTER;
  }

  private static MobileMode parse(String mode) {
    String wanted = mode.toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(candidate -> candidate.label.equals(wanted))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(PROPERTY + " has an unknown mode '" + mode + "'; expected "
            + Arrays.stream(values()).map(MobileMode::label).collect(Collectors.joining(", "))));
  }
}
