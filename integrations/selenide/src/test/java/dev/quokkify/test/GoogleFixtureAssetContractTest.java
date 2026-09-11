package dev.quokkify.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class GoogleFixtureAssetContractTest {

  private static final Path GOOGLE_FIXTURE = Path.of(
      "../../tools/environment/assets/nginx/html/google/index.html");
  private static final Pattern EXTERNAL_LOAD_ATTRIBUTE = Pattern.compile(
      "<(?:script|link|img|image|iframe|source|video|audio|object|embed)\\b"
          + "(?:\\s+[A-Za-z_:][A-Za-z0-9:._-]*(?:\\s*=\\s*(?:\\\"[^\\\"]*\\\"|'[^']*'|[^\\s>]+))?)*"
          + "\\s+(?:src|href)\\s*=\\s*(?:\\\"\\s*(?:https?:)?//"
          + "[^\\\">]*\\\"|'\\s*(?:https?:)?//[^' >]*'|(?:https?:)?//[^\\s>]+)",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern EXTERNAL_SRCSET = Pattern.compile(
      "\\bsrcset\\s*=\\s*(?:\\\"[^\\\"]*(?:[,\\s])(?:https?:)?//"
          + "[^\\\"]*\\\"|'[^']*(?:[,\\s])(?:https?:)?//[^']*'|[^\\s>]*"
          + "(?:https?:)?//[^\\s>]+)",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern EXTERNAL_CSS_IMPORT = Pattern.compile(
      "@import\\s+(?:url\\(\\s*)?(?:\\\"|')?(?:https?:)?//",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern EXTERNAL_CSS_RESOURCE = Pattern.compile(
      "url\\(\\s*[\\\"']?\\s*(?:https?:)?//",
      Pattern.CASE_INSENSITIVE);

  @Test
  public void fixtureHasNoExternalLoadBearingResources() throws Exception {
    String fixture = Files.readString(GOOGLE_FIXTURE);

    Assertions.assertThat(Files.exists(GOOGLE_FIXTURE)).isTrue();
    Assertions.assertThat(EXTERNAL_LOAD_ATTRIBUTE.matcher(fixture).find())
        .as("Google fixture must not load external HTML resources")
        .isFalse();
    Assertions.assertThat(EXTERNAL_SRCSET.matcher(fixture).find())
        .as("Google fixture must not load external srcset resources")
        .isFalse();
    Assertions.assertThat(EXTERNAL_CSS_IMPORT.matcher(fixture).find())
        .as("Google fixture must not import external CSS resources")
        .isFalse();
    Assertions.assertThat(EXTERNAL_CSS_RESOURCE.matcher(fixture).find())
        .as("Google fixture must not load external CSS resources")
        .isFalse();
  }

  @Test
  public void externalResourcePatternsCoverAllSupportedHtmlAndCssForms() {
    String adversarialFixture = """
        <script src=https://cdn.example/script.js></script>
        <link href='//cdn.example/style.css'>
        <img alt="1 > 0" src="https://cdn.example/image.png">
        <img srcset="/local.png 1x, https://cdn.example/2x.png 2x">
        <style>@import "https://cdn.example/import.css"; background: url(//cdn.example/image.png)</style>
        """;

    Assertions.assertThat(EXTERNAL_LOAD_ATTRIBUTE.matcher(adversarialFixture).find()).isTrue();
    Assertions.assertThat(EXTERNAL_SRCSET.matcher(adversarialFixture).find()).isTrue();
    Assertions.assertThat(EXTERNAL_CSS_IMPORT.matcher(adversarialFixture).find()).isTrue();
    Assertions.assertThat(EXTERNAL_CSS_RESOURCE.matcher(adversarialFixture).find()).isTrue();
  }
}
