package dev.quokkify.flutter.locator;

import dev.quokkify.flutter.FlutterBy;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LocatorsTest {

  @Test
  public void describesFlutterKey() {
    assertThat(Locators.describe(FlutterBy.key("login_button"))).isEqualTo("flutter key \"login_button\"");
  }

  @Test
  public void parsesEachFlutterStrategy() {
    assertThat(Locators.parse(FlutterBy.text("Sign in")))
        .isEqualTo(new LocatorSpec(Strategy.TEXT, "Sign in", null, null));
    assertThat(Locators.parse(FlutterBy.semanticsLabel("Settings icon")).strategy())
        .isEqualTo(Strategy.SEMANTICS_LABEL);
    assertThat(Locators.parse(FlutterBy.type("ElevatedButton")).value()).isEqualTo("ElevatedButton");
    assertThat(Locators.parse(FlutterBy.textContaining("Item")).strategy()).isEqualTo(Strategy.TEXT_CONTAINING);
  }

  @Test
  public void parsesDescendantBothSides() {
    By by = FlutterBy.descendant(FlutterBy.key("card_b"), FlutterBy.text("Details"));
    LocatorSpec spec = Locators.parse(by);
    assertThat(spec.strategy()).isEqualTo(Strategy.DESCENDANT);
    assertThat(spec.of()).isEqualTo(new LocatorSpec(Strategy.KEY, "card_b", null, null));
    assertThat(spec.matching()).isEqualTo(new LocatorSpec(Strategy.TEXT, "Details", null, null));
    assertThat(Locators.describe(by))
        .isEqualTo("flutter descendant(of: flutter key \"card_b\", matching: flutter text \"Details\")");
  }

  @Test
  public void parsesAncestorBothSides() {
    LocatorSpec spec = Locators.parse(FlutterBy.ancestor(FlutterBy.type("Card"), FlutterBy.text("Details")));
    assertThat(spec.strategy()).isEqualTo(Strategy.ANCESTOR);
    assertThat(spec.of()).isEqualTo(new LocatorSpec(Strategy.TYPE, "Card", null, null));
    assertThat(spec.matching()).isEqualTo(new LocatorSpec(Strategy.TEXT, "Details", null, null));
  }

  @Test
  public void nativeAndUnknownLocators() {
    assertThat(Locators.parse(AppiumBy.id("login_email")).strategy()).isEqualTo(Strategy.ID);
    assertThat(Locators.parse(AppiumBy.accessibilityId("Settings icon")).strategy())
        .isEqualTo(Strategy.ACCESSIBILITY_ID);
    assertThat(Locators.parse(By.xpath("//a")).strategy()).isEqualTo(Strategy.OTHER);
    assertThat(Locators.describe(By.xpath("//a"))).isEqualTo("By.xpath: //a");
  }
}
