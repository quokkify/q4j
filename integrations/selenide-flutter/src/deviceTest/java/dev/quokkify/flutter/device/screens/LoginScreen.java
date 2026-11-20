package dev.quokkify.flutter.device.screens;

import dev.quokkify.flutter.device.ModeLocators;
import dev.quokkify.impl.Page;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.appium.SelenideAppium;

public class LoginScreen implements Page {

  private final ModeLocators locators;

  public LoginScreen(ModeLocators locators) {
    this.locators = locators;
  }

  public ModeLocators locators() {
    return locators;
  }

  public SelenideElement email() {
    return SelenideAppium.$(locators.id("login_email"));
  }

  public SelenideElement password() {
    return SelenideAppium.$(locators.id("login_password"));
  }

  public SelenideElement loginButton() {
    return SelenideAppium.$(locators.button("login_button"));
  }

  /**
   * A button keyed directly on its {@code ElevatedButton} (no {@code Tagged} wrapper); natively {@code MergeSemantics}
   * folds its Semantics identifier into the button node.
   */
  public SelenideElement directButton() {
    return SelenideAppium.$(locators.id("direct_button"));
  }

  public SelenideElement welcome() {
    return SelenideAppium.$(locators.textOf("login_welcome"));
  }

  /**
   * Focuses the field before typing: natively Flutter applies an accessibility setText only to a focused field.
   */
  public void type(SelenideElement field, String value) {
    field.click();
    field.setValue(value);
  }
}
