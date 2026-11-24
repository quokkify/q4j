package dev.quokkify.flutter.device.scenarios;

import dev.quokkify.flutter.device.BaseMobileTest;
import dev.quokkify.flutter.device.MobileMode;
import dev.quokkify.flutter.device.screens.DuplicatesScreen;

import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class DuplicatesTest extends BaseMobileTest {

  @Factory
  public static Object[] create() {
    return MobileMode.fromProperty().stream().map(DuplicatesTest::new).toArray();
  }

  public DuplicatesTest(MobileMode mode) {
    super(mode);
  }

  @Test(description = "The Details button inside one of two identical cards is tapped")
  public void opensDetailsOfSecondCard() {
    DuplicatesScreen screen = home().openDuplicates();
    screen.openDetailsOf("card_b");
    screen.result().shouldHave(locators.shows("Opened B"));
  }
}
