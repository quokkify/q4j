package dev.quokkify.flutter.device.screens;

import dev.quokkify.flutter.device.ModeLocators;
import dev.quokkify.impl.Page;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.appium.SelenideAppium;

public class DuplicatesScreen implements Page {

  private final ModeLocators locators;

  public DuplicatesScreen(ModeLocators locators) {
    this.locators = locators;
  }

  public void openDetailsOf(String cardId) {
    SelenideAppium.$(locators.descendant(locators.id(cardId), locators.text("Details"))).click();
  }

  public SelenideElement result() {
    return SelenideAppium.$(locators.textOf("details_result"));
  }
}
