package dev.quokkify.test;

import java.time.Duration;
import java.util.Map;

import dev.quokkify.page.local.DelayedTablePage;

import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class UiHorizontalTableTest extends BaseTest {

  @TmsLink("UI_ID_5")
  @Test(description = "Verify local HORIZONTAL TABLE and DYNAMIC HORIZONTAL TABLE rows")
  public void testTable() {
    DelayedTablePage page = openPage();

    page.getHorizontalTableRow(DelayedTablePage.HorizontalHeader.NAME, Duration.ofSeconds(5))
        .verifyRow("Bill Gates");
    page.getHorizontalTableRow(DelayedTablePage.HorizontalHeader.TELEPHONE_1, Duration.ofSeconds(5))
        .verifyRow("555 77 854");
    page.getDynamicHorizontalTableRow(
            DelayedTablePage.DynamicHorizontalHeader.TELEPHONE_2, Duration.ofSeconds(5))
        .verifyRow("555 77 855");
    Assertions.assertThat(page.getHorizontalTableValues())
        .containsExactlyInAnyOrderEntriesOf(Map.of(
            "Name", "Bill Gates",
            "Telephone 1", "555 77 854",
            "Telephone 2", "555 77 855"));
  }

  @Test(description = "Verify missing local HORIZONTAL TABLE row is reported")
  public void testMissingRow() {
    DelayedTablePage page = openPage();

    Assertions.assertThat(page.isHorizontalTableRowExist("Missing Header"))
        .isFalse();
  }

  private DelayedTablePage openPage() {
    return com.codeborne.selenide.Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html", DelayedTablePage.class);
  }
}
