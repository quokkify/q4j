package dev.quokkify.flutter.device.scenarios;

import dev.quokkify.flutter.device.BaseMobileTest;
import dev.quokkify.flutter.device.MobileMode;
import dev.quokkify.flutter.device.screens.LongListScreen;

import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class LongListTest extends BaseMobileTest {

  @Factory
  public static Object[] create() {
    return MobileMode.fromProperty().stream().map(LongListTest::new).toArray();
  }

  public LongListTest(MobileMode mode) {
    super(mode);
  }

  @Test(description = "An item far down a lazily built list is scrolled into view and tapped")
  public void selectsItemOutOfView() {
    LongListScreen screen = home().openLongList();
    screen.scrollTo(80).click();
    screen.selected().shouldHave(locators.shows("Selected 80"));
  }
}
