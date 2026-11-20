package dev.quokkify.elements.table;

import java.util.List;

import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SnapshotLocatorTest {

  @Test
  public void translatesXpath() {
    assertThat(SnapshotLocator.of(By.xpath("./tbody/tr[td]")))
        .isEqualTo(new SnapshotLocator("xpath", "./tbody/tr[td]"));
  }

  @Test
  public void translatesXpathUnion() {
    String headers = TableLayout.xpath(TableLayout.html().headers()).orElseThrow();

    assertThat(SnapshotLocator.of(TableLayout.html().headers())).isEqualTo(new SnapshotLocator("xpath", headers));
    assertThat(headers).contains(" | ");
  }

  @Test
  public void translatesCss() {
    assertThat(SnapshotLocator.of(By.cssSelector(":scope > .data-row")))
        .isEqualTo(new SnapshotLocator("css", ":scope > .data-row"));
  }

  @Test
  public void rejectsOtherBy() {
    assertThatThrownBy(() -> SnapshotLocator.of(By.id("x")))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("snapshot() supports By.xpath and By.cssSelector, got: By.id: x");
    assertThatThrownBy(() -> SnapshotLocator.of(By.className("x")))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("By.className: x");
  }

  @Test
  public void scriptArgShape() {
    assertThat(new SnapshotLocator("css", ".chip").toScriptArg()).isEqualTo(List.of("css", ".chip"));
  }
}
