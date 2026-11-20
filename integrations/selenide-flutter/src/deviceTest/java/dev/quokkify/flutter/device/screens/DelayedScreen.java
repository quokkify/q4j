package dev.quokkify.flutter.device.screens;

import dev.quokkify.flutter.device.ModeLocators;
import dev.quokkify.impl.Page;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.appium.SelenideAppium;

public class DelayedScreen implements Page {

  private final ModeLocators locators;

  public DelayedScreen(ModeLocators locators) {
    this.locators = locators;
  }

  public void load() {
    SelenideAppium.$(locators.id("delayed_load")).click();
  }

  public SelenideElement result() {
    return SelenideAppium.$(locators.textOf("delayed_result"));
  }
}
