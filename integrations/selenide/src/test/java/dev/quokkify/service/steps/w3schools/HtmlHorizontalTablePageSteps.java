package dev.quokkify.service.steps.w3schools;

import dev.quokkify.model.PageSteps;
import dev.quokkify.page.w3school.HtmlHorizontalTablePage;
import dev.quokkify.service.verifications.w3schools.HtmlHorizontalTablePageVerification;

import io.qameta.allure.Step;

public class HtmlHorizontalTablePageSteps
    extends PageSteps<HtmlHorizontalTablePageSteps, HtmlHorizontalTablePageVerification, HtmlHorizontalTablePage> {

  public HtmlHorizontalTablePageSteps(HtmlHorizontalTablePage page) {
    super.verification = new HtmlHorizontalTablePageVerification(this, page);
    super.page = page;
  }

  @Step("Accept Terms")
  public HtmlHorizontalTablePageSteps acceptTerms() {
    page.clickAcceptTermsButtonIfDisplayed();
    return this;
  }
}
