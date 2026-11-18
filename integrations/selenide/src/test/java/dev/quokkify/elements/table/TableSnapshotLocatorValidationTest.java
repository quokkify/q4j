package dev.quokkify.elements.table;

import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TableSnapshotLocatorValidationTest {

  @Test
  public void rejectsUnsupportedLocatorBeforeBrowserCall() {
    Table byId = Table.of($("#t"), TableLayout.of(By.id("r"), By.xpath("./td"), By.xpath("./th")));
    Table html = Table.of($("#t"), TableLayout.html());

    assertThatThrownBy(byId::snapshot)
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("By.id: r");
    assertThatThrownBy(() -> html.snapshot(By.name("chip")))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("By.name: chip");
  }
}
