package dev.quokkify.test;

import java.util.Map;

import dev.quokkify.page.w3school.HtmlTablesPage;

import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

public class UiTableTest extends BaseTest {

  @TmsLink("UI_ID_2")
  @Test(description = "Verify 'TABLE'")
  public void testTableDeprecated() {
    Firm firm = new Firm("Ernst Handel", "Roland Mendel", "Austria");

    w3SchoolsNavigationSteps.openHtmlTablePage()
        .acceptTerms()
        .verify()
        .verifyTableRow(firm.company(), firm);
  }

  @TmsLink("UI_ID_3")
  @Test(description = "Verify 'TABLE'")
  public void testTable() {
    String notFullCountryName = "Austr";
    Firm firm = new Firm("Ernst Handel", "Roland Mendel", notFullCountryName);

    w3SchoolsNavigationSteps.openHtmlTablePage()
        .acceptTerms()
        .verify()
        .verifyTableRow(firm.company(), firm);
  }

  @TmsLink("UI_ID_4")
  @Test(description = "Verify 'Cell' in 'Row' found by several values")
  public void testVerifyRow() {
    Firm firm = new Firm("Ernst Handel", "Roland Mendel", "Austria");

    w3SchoolsNavigationSteps.openHtmlTablePage()
        .acceptTerms()
        .verify()
        .verifyTableRow(
            Map.of(
                HtmlTablesPage.Header.COMPANY, firm.company(),
                HtmlTablesPage.Header.COUNTRY, firm.country()),
            firm);
  }

  public record Firm(String company, String contact, String country) {
  }
}
