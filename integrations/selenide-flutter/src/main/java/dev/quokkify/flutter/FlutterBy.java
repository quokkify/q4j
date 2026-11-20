package dev.quokkify.flutter;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

public final class FlutterBy {

  private FlutterBy() {
  }

  public static By key(String key) {
    return AppiumBy.flutterKey(key);
  }

  public static By text(String text) {
    return AppiumBy.flutterText(text);
  }

  public static By textContaining(String text) {
    return AppiumBy.flutterTextContaining(text);
  }

  public static By type(String type) {
    return AppiumBy.flutterType(type);
  }

  public static By semanticsLabel(String label) {
    return AppiumBy.flutterSemanticsLabel(label);
  }

  public static By descendant(By of, By matching) {
    return AppiumBy.flutterDescendant(flutter(of), flutter(matching));
  }

  public static By ancestor(By of, By matching) {
    return AppiumBy.flutterAncestor(flutter(of), flutter(matching));
  }

  private static AppiumBy.FlutterBy flutter(By by) {
    if (by instanceof AppiumBy.FlutterBy flutterBy) {
      return flutterBy;
    }
    throw new IllegalArgumentException("Flutter locator expected but got: " + by);
  }
}
