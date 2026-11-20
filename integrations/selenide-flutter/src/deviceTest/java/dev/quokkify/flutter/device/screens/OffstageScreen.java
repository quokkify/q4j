package dev.quokkify.flutter.device.screens;

import dev.quokkify.flutter.device.MobileMode;
import dev.quokkify.flutter.device.ModeLocators;
import dev.quokkify.flutter.device.NotReachableInModeException;
import dev.quokkify.impl.Page;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.appium.SelenideAppium;

public class OffstageScreen implements Page {

  private final ModeLocators locators;

  public OffstageScreen(ModeLocators locators) {
    this.locators = locators;
  }

  public SelenideElement hiddenBanner() {
    return SelenideAppium.$(locators.id("hidden_banner"));
  }

  /**
   * The button under the opaque cover. Natively it is unreachable as a visibility check: the accessibility tree has
   * no notion of occlusion and reports the covered button as displayed.
   */
  public SelenideElement coveredButton() {
    if (locators.mode() == MobileMode.NATIVE) {
      throw new NotReachableInModeException(locators.mode(),
          "occlusion (the accessibility tree reports a widget under an opaque cover as displayed)");
    }
    return SelenideAppium.$(locators.id("covered_button"));
  }
}
