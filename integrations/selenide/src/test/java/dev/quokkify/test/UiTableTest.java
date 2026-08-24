package dev.quokkify.test;

import java.time.Duration;
import java.util.Map;

import dev.quokkify.ex.TableRowException;
import dev.quokkify.page.local.DelayedTablePage;

import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class UiTableTest extends BaseTest {

  private static final String TABLE_FIXTURE_PATH = "/table/delayed-table.html";

  @TmsLink("UI_ID_3")
  @Test(description = "Verify local TABLE row lookup, cells, and column values")
  public void testTable() {
    DelayedTablePage page = openPage();
    Firm firm = new Firm("Ernst Handel", "Roland Mendel", "Austria");

    page.getTableRow(DelayedTablePage.Header.COMPANY, firm.company(), Duration.ofSeconds(5))
        .verifyCell(DelayedTablePage.Header.CONTACT, firm.contact());
    page.getTableRow(DelayedTablePage.Header.COMPANY, firm.company(), Duration.ofSeconds(5))
        .verifyCell(DelayedTablePage.Header.COUNTRY, firm.country());
    Assertions.assertThat(page.getTableColumnValues(DelayedTablePage.Header.COMPANY))
        .containsExactly("Alfreds Futterkiste", "Ernst Handel");
  }

  @TmsLink("UI_ID_4")
  @Test(description = "Verify local TABLE map lookup")
  public void testVerifyRow() {
    DelayedTablePage page = openPage();

    page.getTableRow(Map.of(DelayedTablePage.Header.COMPANY, "Ernst Handel",
            DelayedTablePage.Header.COUNTRY, "Austria"), Duration.ofSeconds(5))
        .verifyCell(DelayedTablePage.Header.CONTACT, "Roland Mendel");
  }

  @Test(description = "Verify DYNAMIC TABLE maps displayed headers and FLEX TABLE excludes its header row")
  public void testDynamicAndFlexTables() {
    DelayedTablePage page = openPage();

    page.getDynamicTableRow(DelayedTablePage.DynamicHeader.COMPANY, "Ernst Handel", Duration.ofSeconds(5))
        .verifyCell(DelayedTablePage.DynamicHeader.COUNTRY, "Austria");
    page.getFlexTableRow(DelayedTablePage.Header.COMPANY, "Ernst Handel", Duration.ofSeconds(5))
        .verifyCell(DelayedTablePage.Header.COUNTRY, "Austria");
    Assertions.assertThat(page.getFlexTableColumnValues(DelayedTablePage.Header.COMPANY))
        .containsExactly("Alfreds Futterkiste", "Ernst Handel");
    Assertions.assertThatThrownBy(() -> page.getFlexTableRow(
            DelayedTablePage.Header.COMPANY, "Company", Duration.ofMillis(600)))
        .isInstanceOf(TableRowException.class)
        .hasMessageContaining("Company");
  }

  @Test(description = "Verify missing local TABLE row reports a negative lookup")
  public void testMissingTableRow() {
    DelayedTablePage page = openPage();

    Assertions.assertThatThrownBy(() -> page.getTableRow(
            DelayedTablePage.Header.COMPANY, "Missing Company", Duration.ofMillis(600)))
        .isInstanceOf(TableRowException.class)
        .hasMessageContaining("Missing Company")
        .hasMessageContaining("Company");
  }

  private DelayedTablePage openPage() {
    return com.codeborne.selenide.Selenide.open(APP_CONFIG.baseUrl() + TABLE_FIXTURE_PATH, DelayedTablePage.class);
  }

  public record Firm(String company, String contact, String country) {
  }
}
