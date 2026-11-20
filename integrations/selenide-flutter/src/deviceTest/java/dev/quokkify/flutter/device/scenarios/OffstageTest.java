package dev.quokkify.flutter.device.scenarios;

import dev.quokkify.flutter.device.BaseMobileTest;
import dev.quokkify.flutter.device.MobileMode;

import com.codeborne.selenide.Condition;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class OffstageTest extends BaseMobileTest {

  @Factory
  public static Object[] create() {
    return MobileMode.fromProperty().stream().map(OffstageTest::new).toArray();
  }

  public OffstageTest(MobileMode mode) {
    super(mode);
  }

  @Test(description = "An offstage widget is not visible")
  public void offstageBannerIsNotVisible() {
    home().openOffstage().hiddenBanner().shouldNotBe(Condition.visible);
  }

  @Test(description = "A button under an opaque cover is not visible")
  public void coveredButtonIsNotVisible() {
    home().openOffstage().coveredButton().shouldNotBe(Condition.visible);
  }
}
