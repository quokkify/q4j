package dev.quokkify.flutter.tree;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WidgetTreeParserTest {

  static String resource(String path) throws IOException {
    try (InputStream in = WidgetTreeParserTest.class.getClassLoader().getResourceAsStream(path)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  public void parsesFixtureTree() throws IOException {
    WidgetNode root = WidgetTreeParser.parse(resource("trees/login-screen.json"));
    assertThat(root.type()).isEqualTo("MaterialApp");
    assertThat(root.flatten().map(WidgetNode::key))
        .contains("login_email", "login_password", "login_button", "hidden_banner", "login_form");
    assertThat(root.flatten().filter(n -> "login_button".equals(n.key())).findFirst().orElseThrow().enabled())
        .isFalse();
  }

  @Test
  public void parsesRecordedTree() throws IOException {
    WidgetNode root = WidgetTreeParser.parse(resource("trees/recorded-login.json"));

    assertThat(root.type()).isEqualTo("Scaffold");
    assertThat(root.flatten().map(WidgetNode::key)).contains("login_email", "login_password", "login_button");
  }

  @Test
  public void normalizesFlutterKeyToString() {
    assertThat(WidgetTreeParser.normalizeKey("[<'login'>]")).isEqualTo("login");
    assertThat(WidgetTreeParser.normalizeKey("[<login>]")).isEqualTo("login");
    assertThat(WidgetTreeParser.normalizeKey(null)).isNull();
    assertThat(WidgetTreeParser.normalizeKey("[GlobalKey#1a2b3]")).isEqualTo("[GlobalKey#1a2b3]");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void acceptsScriptResultShapes() throws IOException {
    Map<String, Object> map = new ObjectMapper().readValue(resource("trees/login-screen.json"), Map.class);
    assertThat(WidgetTreeParser.fromScriptResult(map).type()).isEqualTo("MaterialApp");
    assertThat(WidgetTreeParser.fromScriptResult(List.of(map)).type()).isEqualTo("MaterialApp");
    assertThat(WidgetTreeParser.fromScriptResult(resource("trees/login-screen.json")).type())
        .isEqualTo("MaterialApp");
  }

  @Test
  public void missingStateDefaultsToVisibleAndEnabled() {
    WidgetNode node = WidgetTreeParser.parse("{\"type\":\"Text\",\"children\":[]}");
    assertThat(node.visible()).isTrue();
    assertThat(node.enabled()).isTrue();
    assertThat(node.focused()).isFalse();
  }

  @Test
  public void partialOrNullStateFieldsDefaultToVisibleAndEnabled() {
    WidgetNode partial = WidgetTreeParser.parse("{\"type\":\"Text\",\"state\":{\"focused\":true}}");
    assertThat(partial.visible()).isTrue();
    assertThat(partial.enabled()).isTrue();
    assertThat(partial.focused()).isTrue();
    WidgetNode nulls = WidgetTreeParser.parse("{\"type\":\"Text\",\"state\":{\"visible\":null,\"enabled\":null}}");
    assertThat(nulls.visible()).isTrue();
    assertThat(nulls.enabled()).isTrue();
  }
}
