package dev.quokkify.flutter.device.scenarios;

import dev.quokkify.flutter.device.BaseMobileTest;
import dev.quokkify.flutter.device.MobileMode;
import dev.quokkify.flutter.device.steps.LoginScreenSteps;

import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class LoginTest extends BaseMobileTest {

  private static final String EMAIL = "user@example.com";

  @Factory
  public static Object[] create() {
    return MobileMode.fromProperty().stream().map(LoginTest::new).toArray();
  }

  public LoginTest(MobileMode mode) {
    super(mode);
  }

  @Test(description = "Login form enables its button only for valid input and greets the user")
  public void logsIn() {
    new LoginScreenSteps(home().openLogin())
        .verify().loginButtonIsDisabled().backToSteps()
        .typeEmail(EMAIL)
        .verify().loginButtonIsDisabled().backToSteps()
        .typePassword("secret1")
        .verify().loginButtonIsEnabled().backToSteps()
        .submit()
        .verify().welcomes(EMAIL);
  }
}
