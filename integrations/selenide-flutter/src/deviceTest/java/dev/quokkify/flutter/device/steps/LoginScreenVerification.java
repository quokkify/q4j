package dev.quokkify.flutter.device.steps;

import dev.quokkify.flutter.device.screens.LoginScreen;
import dev.quokkify.model.Verification;

import com.codeborne.selenide.Condition;
import io.qameta.allure.Step;

public class LoginScreenVerification extends Verification<LoginScreenSteps, LoginScreenVerification, LoginScreen> {

  public LoginScreenVerification(LoginScreenSteps steps, LoginScreen page) {
    super(steps, page);
  }

  @Step("Verify the login button is disabled")
  public LoginScreenVerification loginButtonIsDisabled() {
    page.loginButton().shouldBe(Condition.visible, getTimeout()).shouldBe(Condition.disabled, getTimeout());
    return this;
  }

  @Step("Verify the login button is enabled")
  public LoginScreenVerification loginButtonIsEnabled() {
    page.loginButton().shouldBe(Condition.enabled, getTimeout());
    return this;
  }

  @Step("Verify the welcome text greets '{email}'")
  public LoginScreenVerification welcomes(String email) {
    page.welcome().shouldHave(page.locators().shows("Welcome, " + email), getTimeout());
    return this;
  }
}
