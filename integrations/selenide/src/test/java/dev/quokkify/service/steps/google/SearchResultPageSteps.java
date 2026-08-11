package dev.quokkify.service.steps.google;

import dev.quokkify.model.PageSteps;
import dev.quokkify.page.google.SearchResultPage;
import dev.quokkify.service.verifications.google.SearchResultPageVerification;

import io.qameta.allure.Step;

public class SearchResultPageSteps
    extends PageSteps<SearchResultPageSteps, SearchResultPageVerification, SearchResultPage> {

  public SearchResultPageSteps(SearchResultPage page) {
    super.verification = new SearchResultPageVerification(this, page);
    super.page = page;
  }

  @Step("Click on search result link with '{searchResultLinkText}' link text")
  public SearchResultPageSteps clickOnSearchResultLink(String searchResultLinkText) {
    page.clickOnSearchResultLinkByLinkText(searchResultLinkText);
    return this;
  }
}
