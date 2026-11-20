package dev.quokkify.flutter.device.scenarios;

import dev.quokkify.flutter.device.BaseMobileTest;
import dev.quokkify.flutter.device.MobileMode;
import dev.quokkify.flutter.device.screens.LabelOnlyScreen;

import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class LabelOnlyTest extends BaseMobileTest {

  @Factory
  public static Object[] create() {
    return MobileMode.fromProperty().stream().map(LabelOnlyTest::new).toArray();
  }

  public LabelOnlyTest(MobileMode mode) {
    super(mode);
  }

  @Test(description = "An icon button with only a semantics label is tapped")
  public void opensSettingsByLabel() {
    LabelOnlyScreen screen = home().openLabelOnly();
    screen.openSettings();
    screen.result().shouldHave(locators.shows("Settings opened"));
  }
}
