package dev.quokkify.service.verifications.google;

import java.util.List;

import dev.quokkify.annotation.PageUrl;
import dev.quokkify.page.google.SearchResultPage;
import dev.quokkify.service.steps.google.SearchResultPageSteps;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebElementsCondition;
import org.assertj.core.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.openqa.selenium.WebElement;
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

      ArgumentCaptor<WebElementsCondition> conditionCaptor =
          ArgumentCaptor.forClass(WebElementsCondition.class);
      Mockito.verify(results).shouldHave(conditionCaptor.capture());

      WebElementsCondition condition = conditionCaptor.getValue();
      Assertions.assertThat(condition.check(null, List.of()).verdict())
          .isEqualTo(CheckResult.Verdict.REJECT);
      Assertions.assertThat(condition.check(null, List.of(Mockito.mock(WebElement.class))).verdict())
          .isEqualTo(CheckResult.Verdict.ACCEPT);
    }
  }
}
