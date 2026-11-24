package dev.quokkify.flutter.device.screens;

import dev.quokkify.flutter.device.ModeLocators;
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

  public SelenideElement coveredButton() {
    return SelenideAppium.$(locators.id("covered_button"));
  }
}
