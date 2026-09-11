package dev.quokkify.service.verifications.google;

import dev.quokkify.page.google.SearchResultPage;
import dev.quokkify.service.steps.google.SearchResultPageSteps;

import org.mockito.Mockito;
import org.testng.annotations.Test;

public class SearchResultPageVerificationTest {

  @Test
  public void verifySearchResultsExistUsesPageSmartCondition() {
    SearchResultPage page = Mockito.mock(SearchResultPage.class);
    SearchResultPageVerification verification = new SearchResultPageSteps(page).verify();

    verification.verifySearchResultsExist();

    Mockito.verify(page).shouldHaveSearchResults();
  }
}
