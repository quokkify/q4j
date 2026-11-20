package dev.quokkify.flutter.diagnostics;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

import dev.quokkify.flutter.FlutterBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class FailureDiagnosticsTest {

  private static final String MARKER = "\n…[truncated]";

  private JavascriptExecutor js;
  private Map<String, Object> fixtureMap;

  @SuppressWarnings("unchecked")
  private static Map<String, Object> fixture() throws IOException {
    try (InputStream in = FailureDiagnosticsTest.class.getClassLoader()
        .getResourceAsStream("trees/login-screen.json")) {
      return new ObjectMapper().readValue(in, Map.class);
    }
  }

  @BeforeMethod
  public void setUp() throws IOException {
    js = mock(JavascriptExecutor.class);
    fixtureMap = fixture();
  }

  @Test
  public void collectsTreeAndSuggestions() {
    when(js.executeScript("flutter: renderTree", Map.of())).thenReturn(fixtureMap);

    Diagnostics d = new FailureDiagnostics(Duration.ofSeconds(1), 1_000_000).collect(js, FlutterBy.key("login_btn"));

    assertThat(d.treeText()).contains("login_button");
    assertThat(d.treeJson()).contains("login_button");
    assertThat(d.suggestions().get(0).node().key()).isEqualTo("login_button");
    assertThat(d.error()).isNull();
    assertThat(d.truncated()).isFalse();
  }

  @Test
  public void diagnosticsTimeoutKeepsOriginalError() {
    when(js.executeScript(anyString(), any())).thenAnswer(i -> {
      Thread.sleep(2_000);
      return fixtureMap;
    });

    Diagnostics d = new FailureDiagnostics(Duration.ofMillis(100), 1_000_000).collect(js, FlutterBy.key("x"));

    assertThat(d.error()).startsWith("TimeoutException");
    assertThat(d.treeJson()).isNull();
    assertThat(d.suggestions()).isEmpty();
  }

  @Test
  public void hugeTreeIsTruncated() {
    when(js.executeScript("flutter: renderTree", Map.of())).thenReturn(fixtureMap);

    Diagnostics d = new FailureDiagnostics(Duration.ofSeconds(1), 200).collect(js, FlutterBy.key("login_btn"));

    assertThat(d.truncated()).isTrue();
    assertThat(d.treeJson()).hasSizeLessThanOrEqualTo(200 + MARKER.length()).endsWith(MARKER);
    assertThat(d.treeText()).contains("login_button");
    assertThat(d.suggestions()).isNotEmpty();
  }

  @Test
  public void nativeLocatorFailureSkipsRenderTree() {
    Diagnostics d = new FailureDiagnostics(Duration.ofSeconds(1), 1000).collect(js, AppiumBy.id("login_btn"));

    verifyNoInteractions(js);
    assertThat(d.treeJson()).isNull();
    assertThat(d.treeText()).isNull();
    assertThat(d.suggestions()).isEmpty();
    assertThat(d.error()).isNull();
    assertThat(d.truncated()).isFalse();
  }

  @Test
  public void scriptFailureBecomesErrorNotException() {
    when(js.executeScript("flutter: renderTree", Map.of())).thenThrow(new IllegalStateException("no flutter"));

    Diagnostics d = new FailureDiagnostics(Duration.ofSeconds(1), 1000).collect(js, FlutterBy.key("x"));

    assertThat(d.error()).isEqualTo("IllegalStateException: no flutter");
    assertThat(d.treeJson()).isNull();
  }

  @Test
  public void unparseableTreeKeepsRawJsonAndReportsError() {
    when(js.executeScript("flutter: renderTree", Map.of())).thenReturn("not json");

    Diagnostics d = new FailureDiagnostics(Duration.ofSeconds(1), 1000).collect(js, FlutterBy.key("x"));

    assertThat(d.treeJson()).isEqualTo("not json");
    assertThat(d.treeText()).isNull();
    assertThat(d.error()).isNotNull();
  }

  @Test
  public void rejectsNonPositiveLimits() {
    assertThatThrownBy(() -> new FailureDiagnostics(Duration.ZERO, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FailureDiagnostics(Duration.ofSeconds(1), 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
