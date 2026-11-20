package dev.quokkify.flutter.device.steps;

import dev.quokkify.flutter.device.screens.LoginScreen;
import dev.quokkify.model.PageSteps;

import io.qameta.allure.Step;

public class LoginScreenSteps extends PageSteps<LoginScreenSteps, LoginScreenVerification, LoginScreen> {

  public LoginScreenSteps(LoginScreen page) {
    super.verification = new LoginScreenVerification(this, page);
    super.page = page;
  }

  @Step("Type '{email}' as the email")
  public LoginScreenSteps typeEmail(String email) {
    page.type(page.email(), email);
    return this;
  }

  @Step("Type the password")
  public LoginScreenSteps typePassword(String password) {
    page.type(page.password(), password);
    return this;
  }

  @Step("Submit the login form")
  public LoginScreenSteps submit() {
    page.loginButton().click();
    return this;
  }
}
