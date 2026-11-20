package dev.quokkify.flutter.device.scenarios;

import dev.quokkify.flutter.device.BaseMobileTest;
import dev.quokkify.flutter.device.MobileMode;
import dev.quokkify.flutter.device.screens.LoginScreen;
import dev.quokkify.flutter.device.steps.LoginScreenSteps;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.appium.SelenideAppium;
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

  @Test(groups = {"known-gap-flutter", "known-gap-native"}, description = "The element with the login_button id"
      + " follows the form state (known gap in flutter mode: the keyed KeyedSubtree always reports enabled=false;"
      + " in native mode: the node with the resource-id is a wrapper that is always enabled)")
  public void keyedButtonFollowsFormState() {
    LoginScreen screen = home().openLogin();
    SelenideElement keyed = SelenideAppium.$(locators.id("login_button"));
    keyed.shouldBe(Condition.disabled);
    screen.type(screen.email(), EMAIL);
    screen.type(screen.password(), "secret1");
    keyed.shouldBe(Condition.enabled);
  }
}
