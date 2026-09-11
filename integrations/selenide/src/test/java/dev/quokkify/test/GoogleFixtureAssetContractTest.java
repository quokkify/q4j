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
      "<(?:script|link|img|image|iframe|source|video|audio|object|embed)\\b[^>]*\\b(?:src|srcset|href)\\s*=\\s*[\\\"']\\s*(?:https?:)?//",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
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
    Assertions.assertThat(EXTERNAL_CSS_RESOURCE.matcher(fixture).find())
        .as("Google fixture must not load external CSS resources")
        .isFalse();
  }
}
