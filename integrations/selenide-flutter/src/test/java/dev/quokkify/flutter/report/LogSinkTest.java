package dev.quokkify.flutter.report;

import dev.quokkify.flutter.FlutterVisibility;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogSinkTest {

  // Regression guard: the truncated tree JSON is attached as text/plain and was logged at WARN.
  @Test
  public void omitsTreeJsonByNameEvenWhenMimeTypeIsPlainText() {
    assertThat(LogSink.isOmitted(FlutterVisibility.TREE_JSON, "text/plain")).isTrue();
  }

  @Test
  public void omitsJsonByMimeType() {
    assertThat(LogSink.isOmitted("anything", "application/json")).isTrue();
  }

  @Test
  public void keepsOtherAttachments() {
    assertThat(LogSink.isOmitted("Flutter: widget tree", "text/plain")).isFalse();
  }
}
