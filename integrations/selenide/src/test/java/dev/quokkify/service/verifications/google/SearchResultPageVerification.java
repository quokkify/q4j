package dev.quokkify.service.verifications.google;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import dev.quokkify.model.Verification;
import dev.quokkify.page.google.SearchResultPage;
import dev.quokkify.service.steps.google.SearchResultPageSteps;
import dev.quokkify.util.Waiter;

import com.codeborne.selenide.WebDriverRunner;
import io.qameta.allure.Step;
import org.assertj.core.api.Assertions;

public class SearchResultPageVerification extends Verification<SearchResultPageSteps, SearchResultPageVerification, SearchResultPage> {

  public SearchResultPageVerification(SearchResultPageSteps steps, SearchResultPage page) {
    super(steps, page);
  }

  @Step("Verify that search results exist")
  public SearchResultPageVerification verifySearchResultsExist() {
    Waiter.awaitAssertion(
        () -> Assertions.assertThat(page.getSearchTitlesCount()).as("The search results not exists").isPositive(),
        getTimeout(),
        getPollingInterval());
    return this;
  }

  @Step("Verify opened page url")
  public SearchResultPageVerification verifyOpenedPageUrl(String expectedUrl) {
    String actualUrl = Objects.requireNonNull(WebDriverRunner.url(), "Browser url is null: no page is opened");
    if (!actualUrl.startsWith(expectedUrl)) {
      String normalizedActual = dropPort(actualUrl);
      String normalizedExpected = dropPort(expectedUrl);
      Assertions.assertThat(normalizedActual).as("Page url is incorrect").startsWith(normalizedExpected);
      return this;
    }
    Assertions.assertThat(actualUrl).as("Page url is incorrect").startsWith(expectedUrl);
    return this;
  }

  private static String dropPort(String url) {
    try {
      URI uri = new URI(url);
      if (uri.getPort() == -1) {
        return url;
      }
      return new URI(
          uri.getScheme(),
          uri.getUserInfo(),
          uri.getHost(),
          -1,
          uri.getPath(),
          uri.getQuery(),
          uri.getFragment()).toString();
    } catch (URISyntaxException e) {
      return url;
    }
  }
}
