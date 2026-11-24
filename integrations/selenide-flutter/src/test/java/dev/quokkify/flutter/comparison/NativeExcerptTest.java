package dev.quokkify.flutter.comparison;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NativeExcerptTest {

  private static final String SAMPLE = """
      <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
      <hierarchy index="0" class="hierarchy" rotation="0">
        <android.widget.FrameLayout index="0" class="android.widget.FrameLayout" text="" bounds="[0,0][1080,2400]">
          <android.widget.Button index="0" class="android.widget.Button" text=""
            content-desc="Back" bounds="[11,139][137,265]" />
          <android.view.View index="1" class="android.view.View" text=""
            resource-id="covered_button" content-desc="" bounds="[408,1254][672,1380]" />
          <android.widget.TextView index="2" class="android.widget.TextView" text="Hello" bounds="[1,2][3,4]" />
        </android.widget.FrameLayout>
      </hierarchy>
      """;

  @Test
  public void keepsOnlyNodesWithIdDescOrTextInCompactForm() {
    assertThat(NativeExcerpt.of(SAMPLE, 15).lines()).containsExactly(
        "Button desc=\"Back\" [11,139][137,265]",
        "View id=\"covered_button\" [408,1254][672,1380]",
        "TextView text=\"Hello\" [1,2][3,4]");
  }

  @Test
  public void truncatesWithMoreNodesMarker() {
    assertThat(NativeExcerpt.of(SAMPLE, 1).lines()).containsExactly(
        "Button desc=\"Back\" [11,139][137,265]", "(2 more nodes)");
  }

  @Test
  public void reportsUnparsableSource() {
    assertThat(NativeExcerpt.of("not xml", 15)).startsWith("(page source could not be parsed");
  }
}
