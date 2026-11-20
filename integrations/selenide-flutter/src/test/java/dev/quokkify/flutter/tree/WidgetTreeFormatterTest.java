package dev.quokkify.flutter.tree;

import java.io.IOException;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WidgetTreeFormatterTest {

  @Test
  public void formatsOneLinePerNodeWithState() throws IOException {
    String text = WidgetTreeFormatter.format(
        WidgetTreeParser.parse(WidgetTreeParserTest.resource("trees/login-screen.json")));
    assertThat(text).contains("  ElevatedButton key=\"login_button\" [disabled]");
    assertThat(text).contains("Text key=\"hidden_banner\" text=\"Hidden banner\" [hidden]");
    assertThat(text).contains("Icon label=\"Settings icon\" [zero-size]");
  }
}
