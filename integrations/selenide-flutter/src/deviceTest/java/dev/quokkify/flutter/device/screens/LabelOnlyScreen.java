package dev.quokkify.flutter.device.screens;

import dev.quokkify.flutter.device.ModeLocators;
import dev.quokkify.impl.Page;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.appium.SelenideAppium;

public class LabelOnlyScreen implements Page {

  private final ModeLocators locators;

  public LabelOnlyScreen(ModeLocators locators) {
    this.locators = locators;
  }

  public void openSettings() {
    SelenideAppium.$(locators.label("Settings icon")).click();
  }

  public SelenideElement result() {
    return SelenideAppium.$(locators.textOf("label_result"));
  }
}
