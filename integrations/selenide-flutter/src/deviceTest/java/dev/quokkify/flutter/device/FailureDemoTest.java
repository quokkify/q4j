package dev.quokkify.flutter.device;

import com.codeborne.selenide.appium.SelenideAppium;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * Intentional failures that show the Flutter diagnostics attached to the report; both tests are expected to fail.
 */
public class FailureDemoTest extends BaseMobileTest {

  @Factory
  public static Object[] create() {
    return MobileMode.fromProperty().stream().map(FailureDemoTest::new).toArray();
  }

  public FailureDemoTest(MobileMode mode) {
    super(mode);
  }

  @Test(description = "Expected to fail: taps a mistyped key (login_btn instead of login_button)")
  public void tapsMistypedKey() {
    home().openLogin();
    SelenideAppium.$(locators.id("login_btn")).click();
  }

  @Test(description = "Expected to fail: taps a banner that is offstage")
  public void tapsOffstageBanner() {
    home().openOffstage().hiddenBanner().click();
  }
}
