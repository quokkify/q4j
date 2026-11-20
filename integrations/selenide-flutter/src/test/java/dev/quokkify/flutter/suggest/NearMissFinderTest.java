package dev.quokkify.flutter.suggest;

import java.io.IOException;
import java.util.List;

import dev.quokkify.flutter.FlutterBy;
import dev.quokkify.flutter.locator.Locators;
import dev.quokkify.flutter.tree.Rect;
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
    assertThat(NearMissFinder.find(Locators.parse(FlutterBy.key("login")), tree, 2)).hasSize(2);
  }

  @Test
  public void nativeStrategyHasNoSuggestions() {
    assertThat(NearMissFinder.find(Locators.parse(AppiumBy.id("login_btn")), tree, 5)).isEmpty();
  }

  private static WidgetNode node(String type, String key, String text, String label, boolean visible, boolean enabled,
                                 Rect rect, WidgetNode... children) {
    return new WidgetNode(type, "StatelessElement", type, 0, key, text, label, null, null, visible, enabled, false,
        rect, List.of(children));
  }

  private static final Rect BOX = new Rect(0, 0, 10, 10);

  @Test
  public void disabledReasonAlone() {
    WidgetNode root = node("Root", null, null, null, true, true, BOX,
        node("Button", "save", null, null, true, false, BOX));
    assertThat(NearMissFinder.find(Locators.parse(FlutterBy.key("save")), root, 5).get(0).reasons())
        .containsExactly("exact key match but disabled");
  }

  @Test
  public void whitespaceOnlyDifference() {
    WidgetNode root = node("Root", null, null, null, true, true, BOX,
        node("Text", null, " Sign in ", null, true, true, BOX));
    NearMiss top = NearMissFinder.find(Locators.parse(FlutterBy.text("Sign in")), root, 5).get(0);
    assertThat(top.reasons()).containsExactly("differs only in whitespace");
    assertThat(top.score()).isEqualTo(0.9);
  }

  @Test
  public void partialMatch() {
    NearMiss top = NearMissFinder.find(Locators.parse(FlutterBy.key("login_button_extra")), tree, 5).get(0);
    assertThat(top.node().key()).isEqualTo("login_button");
    assertThat(top.reasons()).containsExactly("partial match");
  }

  @Test
  public void blankFieldNeverPartialMatches() {
    WidgetNode root = node("Root", null, null, null, true, true, BOX,
        node("Text", "", "", null, true, true, BOX));
    assertThat(NearMissFinder.find(Locators.parse(FlutterBy.key("anything_at_all")), root, 5)).isEmpty();
  }

  @Test
  public void textContainingExplainsHiddenMatch() {
    WidgetNode root = node("Root", null, null, null, true, true, BOX,
        node("Text", null, "Sign in now", null, false, true, BOX));
    assertThat(NearMissFinder.find(Locators.parse(FlutterBy.textContaining("Sign")), root, 5).get(0).reasons())
        .containsExactly("exact text match but hidden");
  }

  @Test
  public void textContainingHittableMatchIsNotSuggested() {
    assertThat(NearMissFinder.find(Locators.parse(FlutterBy.textContaining("Sign")), tree, 5)).isEmpty();
  }

  @Test
  public void typeStrategySuggestsSimilarType() {
    NearMiss top = NearMissFinder.find(Locators.parse(FlutterBy.type("TextFeld")), tree, 5).get(0);
    assertThat(top.node().type()).isEqualTo("TextField");
    assertThat(top.reasons().get(0)).startsWith("similar type");
  }

  @Test
  public void semanticsLabelStrategyExplainsZeroSize() {
    assertThat(NearMissFinder.find(Locators.parse(FlutterBy.semanticsLabel("Settings icon")), tree, 5).get(0)
        .reasons()).containsExactly("exact semantics label match but zero-size");
  }

  @Test
  public void ancestorSearchesAlongAncestorsOfAnchor() {
    List<NearMiss> m = NearMissFinder.find(
        Locators.parse(FlutterBy.ancestor(FlutterBy.key("login_email"), FlutterBy.key("login_fom"))), tree, 5);
    assertThat(m.get(0).node().key()).isEqualTo("login_form");
    assertThat(m.get(0).reasons().get(0)).startsWith("matching: ");
  }

  @Test
  public void ancestorReportsOfSideWhenAnchorMissing() {
    List<NearMiss> m = NearMissFinder.find(
        Locators.parse(FlutterBy.ancestor(FlutterBy.key("login_emal"), FlutterBy.key("login_form"))), tree, 5);
    assertThat(m.get(0).reasons().get(0)).startsWith("of: ");
  }
}
