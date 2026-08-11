package dev.quokkify.service.steps.w3schools;

import dev.quokkify.model.PageSteps;
import dev.quokkify.page.w3school.HtmlTablesPage;
import dev.quokkify.service.verifications.w3schools.HtmlTablesPageVerification;

import io.qameta.allure.Step;

public class HtmlTablesPageSteps extends PageSteps<HtmlTablesPageSteps, HtmlTablesPageVerification, HtmlTablesPage> {

  public HtmlTablesPageSteps(HtmlTablesPage page) {
    super.verification = new HtmlTablesPageVerification(this, page);
    super.page = page;
  }

  @Step("Accept Terms")
  public HtmlTablesPageSteps acceptTerms() {
    page.clickAcceptTermsButtonIfDisplayed();
    return this;
  }
}
