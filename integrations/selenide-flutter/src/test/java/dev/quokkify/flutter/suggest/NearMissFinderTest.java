package dev.quokkify.flutter.suggest;

import java.io.IOException;
import java.util.List;

import dev.quokkify.flutter.FlutterBy;
import dev.quokkify.flutter.locator.Locators;
import dev.quokkify.flutter.tree.WidgetNode;
import dev.quokkify.flutter.tree.WidgetTreeParser;

import io.appium.java_client.AppiumBy;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NearMissFinderTest {

  private WidgetNode tree;

  @BeforeClass
  public void loadTree() throws IOException {
    try (var in = getClass().getClassLoader().getResourceAsStream("trees/login-screen.json")) {
      tree = WidgetTreeParser.parse(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  @Test
  public void typoInKeySuggestsRealKey() {
    List<NearMiss> m = NearMissFinder.find(Locators.parse(FlutterBy.key("login_btn")), tree, 5);
    assertThat(m.get(0).node().key()).isEqualTo("login_button");
    assertThat(m.get(0).reasons()).anyMatch(r -> r.startsWith("similar key"));
  }

  @Test
  public void exactButHiddenExplainsWhy() {
    NearMiss top = NearMissFinder.find(Locators.parse(FlutterBy.key("hidden_banner")), tree, 5).get(0);
    assertThat(top.score()).isEqualTo(1.0);
    assertThat(top.reasons()).containsExactly("exact key match but hidden and zero-size");
  }

  @Test
  public void caseOnlyDifference() {
    assertThat(NearMissFinder.find(Locators.parse(FlutterBy.text("sign in")), tree, 5).get(0).reasons())
        .containsExactly("differs only in case");
  }

  @Test
  public void textFoundAsSemanticsLabel() {
    assertThat(NearMissFinder.find(Locators.parse(FlutterBy.text("Settings icon")), tree, 5).get(0).reasons())
        .contains("value found as semantics label; try FlutterBy.semanticsLabel");
  }

  @Test
  public void descendantReportsFailingSide() {
    List<NearMiss> m = NearMissFinder.find(
        Locators.parse(FlutterBy.descendant(FlutterBy.key("login_from"), FlutterBy.text("Sign in"))), tree, 5);
    assertThat(m.get(0).reasons().get(0)).startsWith("of: ");
  }

  @Test
  public void descendantReportsMatchingSideWhenAnchorFound() {
    List<NearMiss> m = NearMissFinder.find(
        Locators.parse(FlutterBy.descendant(FlutterBy.key("login_form"), FlutterBy.key("login_pasword"))), tree, 5);
    assertThat(m.get(0).node().key()).isEqualTo("login_password");
    assertThat(m.get(0).reasons().get(0)).startsWith("matching: ");
  }

  @Test
  public void respectsLimitAndThreshold() {
    assertThat(NearMissFinder.find(Locators.parse(FlutterBy.key("zzzzzzzzzz")), tree, 5)).isEmpty();
    assertThat(NearMissFinder.find(Locators.parse(FlutterBy.key("login")), tree, 2)).hasSizeLessThanOrEqualTo(2);
  }

  @Test
  public void nativeStrategyHasNoSuggestions() {
    assertThat(NearMissFinder.find(Locators.parse(AppiumBy.id("login_btn")), tree, 5)).isEmpty();
  }
}
