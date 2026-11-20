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

  @Test
  public void lineFormatsSingleNodeWithoutIndentOrChildren() throws IOException {
    WidgetNode button = WidgetTreeParser.parse(WidgetTreeParserTest.resource("trees/login-screen.json")).flatten()
        .filter(n -> "login_button".equals(n.key())).findFirst().orElseThrow();

    String line = WidgetTreeFormatter.line(button);

    assertThat(line).startsWith("ElevatedButton key=\"login_button\" [disabled] @").doesNotContain("\n");
    assertThat(WidgetTreeFormatter.format(button)).startsWith("  ".repeat(button.depth()) + line);
  }
}
