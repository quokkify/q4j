package dev.quokkify.service.verifications.w3schools;

import java.util.Map;

import dev.quokkify.elements.table.model.CheckType;
import dev.quokkify.elements.table.model.RowData;
import dev.quokkify.model.Verification;
import dev.quokkify.page.w3school.HtmlTablesPage;
import dev.quokkify.service.steps.w3schools.HtmlTablesPageSteps;
import dev.quokkify.test.UiTableTest;

import io.qameta.allure.Step;

public class HtmlTablesPageVerification extends Verification<HtmlTablesPageSteps, HtmlTablesPageVerification, HtmlTablesPage> {

  public HtmlTablesPageVerification(HtmlTablesPageSteps steps, HtmlTablesPage page) {
    super(steps, page);
  }

  @Step("Verify table row")
  public HtmlTablesPageVerification verifyTableRow(String company, UiTableTest.Firm firm) {
    page.getTableRowWithValueInColumn(company)
        .verifyRow(RowData.<HtmlTablesPage.Header>builder()
            .add(CheckType.EQUALS, HtmlTablesPage.Header.CONTACT, firm.contact())
            .add(CheckType.CONTAINS, HtmlTablesPage.Header.COUNTRY, firm.country())
            .build());
    return this;
  }

  @Step("Verify table row found from several values")
  public HtmlTablesPageVerification verifyTableRow(Map<HtmlTablesPage.Header, String> actualRowValues,
                                                   UiTableTest.Firm firm) {
    page.getTableRowWithValuesInColumns(actualRowValues)
        .verifyCell(HtmlTablesPage.Header.CONTACT, firm.contact());
    return this;
  }
}
