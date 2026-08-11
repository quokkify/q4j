package dev.quokkify.service.verifications.w3schools;

import dev.quokkify.model.Verification;
import dev.quokkify.page.w3school.HtmlHorizontalTablePage;
import dev.quokkify.service.steps.w3schools.HtmlHorizontalTablePageSteps;

import io.qameta.allure.Step;

public class HtmlHorizontalTablePageVerification
    extends Verification<HtmlHorizontalTablePageSteps, HtmlHorizontalTablePageVerification, HtmlHorizontalTablePage> {

  public HtmlHorizontalTablePageVerification(HtmlHorizontalTablePageSteps steps, HtmlHorizontalTablePage page) {
    super(steps, page);
  }

  @Step("Verify horizontal table row")
  public HtmlHorizontalTablePageVerification verifyTableRow(HtmlHorizontalTablePage.Header header,
                                                            String expectedRowValue) {
    page.getTableRowByColumn(header).verifyRow(expectedRowValue);
    return this;
  }
}
