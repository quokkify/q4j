package dev.quokkify.flutter.device.screens;

import java.util.Map;

import dev.quokkify.flutter.device.MobileMode;
import dev.quokkify.flutter.device.ModeLocators;
import dev.quokkify.impl.Page;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.appium.SelenideAppium;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.flutter.CanExecuteFlutterScripts;

public class LongListScreen implements Page {

  // Slow drags: the server's default 100 ms drag flings the list past the item between two visibility checks.
  private static final int SCROLL_DELTA = 300;
  private static final int DRAG_DURATION_MS = 600;
  private static final int MAX_SCROLLS = 30;

  private final ModeLocators locators;

  public LongListScreen(ModeLocators locators) {
    this.locators = locators;
  }

  /**
   * Scrolls the list until the item is built and visible: {@code flutter: scrollTillVisible} in flutter mode,
   * {@code UiScrollable} natively.
   */
  public SelenideElement scrollTo(int index) {
    String id = "list_item_" + index;
    if (locators.mode() == MobileMode.FLUTTER) {
      // A raw map: ScrollParameter serializes dragDuration in whole seconds, but the server reads milliseconds.
      ((CanExecuteFlutterScripts) WebDriverRunner.getWebDriver()).executeFlutterCommand("scrollTillVisible", Map.of(
          "finder", Map.of("using", "-flutter key", "value", id),
          "delta", SCROLL_DELTA,
          "dragDuration", DRAG_DURATION_MS,
          "maxScrolls", MAX_SCROLLS));
      return SelenideAppium.$(locators.id(id));
    }
    return SelenideAppium.$(AppiumBy.androidUIAutomator(
        "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId(\""
            + id + "\"))"));
  }

  public SelenideElement selected() {
    return SelenideAppium.$(locators.textOf("list_selected"));
  }
}
