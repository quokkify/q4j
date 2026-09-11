package dev.quokkify.service.verifications.google;

import dev.quokkify.annotation.PageUrl;
import dev.quokkify.page.google.SearchResultPage;
import dev.quokkify.service.steps.google.SearchResultPageSteps;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import org.assertj.core.api.Assertions;
import org.mockito.MockedStatic;
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

  @Test
  public void searchResultPageUsesGoogleFixtureRoute() {
    Assertions.assertThat(SearchResultPage.class.getAnnotation(PageUrl.class).value())
        .isEqualTo("/google/");
  }

  @Test
  public void searchResultPageUsesNativeCollectionSmartCondition() {
    ElementsCollection results = Mockito.mock(ElementsCollection.class);

    try (MockedStatic<Selenide> selenideMock = Mockito.mockStatic(Selenide.class)) {
      selenideMock.when(() -> Selenide.$$("#rso > div")).thenReturn(results);

      new SearchResultPage().shouldHaveSearchResults();

      Mockito.verify(results).shouldHave(Mockito.any());
    }
  }
}
