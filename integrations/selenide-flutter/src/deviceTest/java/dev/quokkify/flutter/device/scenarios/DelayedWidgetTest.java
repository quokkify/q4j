package dev.quokkify.flutter.device.scenarios;

import dev.quokkify.flutter.device.BaseMobileTest;
import dev.quokkify.flutter.device.MobileMode;
import dev.quokkify.flutter.device.screens.DelayedScreen;

import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class DelayedWidgetTest extends BaseMobileTest {

  @Factory
  public static Object[] create() {
    return MobileMode.fromProperty().stream().map(DelayedWidgetTest::new).toArray();
  }

  public DelayedWidgetTest(MobileMode mode) {
    super(mode);
  }

  @Test(description = "A widget that appears two seconds after a tap is found")
  public void waitsForDelayedResult() {
    DelayedScreen screen = home().openDelayed();
    screen.load();
    screen.result().shouldHave(locators.shows("Loaded"));
  }
}
