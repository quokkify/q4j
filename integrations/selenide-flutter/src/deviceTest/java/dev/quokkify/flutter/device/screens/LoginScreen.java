package dev.quokkify.flutter.device.screens;

import dev.quokkify.flutter.device.MobileMode;
import dev.quokkify.flutter.device.ModeLocators;
import dev.quokkify.flutter.device.NotReachableInModeException;
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

  public SelenideElement welcome() {
    return SelenideAppium.$(locators.textOf("login_welcome"));
  }

  public void type(SelenideElement field, String value) {
    if (locators.mode() == MobileMode.NATIVE) {
      // The app runs under the Appium Flutter server's test binding, which mocks the platform text input:
      // native keystrokes and accessibility setText never reach the TextField controller.
      throw new NotReachableInModeException(locators.mode(),
          "typing into a Flutter TextField (the app's test binding mocks the platform text input)");
    }
    field.setValue(value);
  }
}
