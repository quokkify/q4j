package dev.quokkify.test;

import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

public class ElementsCollectionTest extends BaseTest {

  @TmsLink("UI_ID_6")
  @Test(description = "Verify 'CustomElementsCollection'")
  public void testVerifyCustomElementsCollection() {
    String searchLinkText = "Speed Test";
    String searchResultRelativePath = "/external/speedtest/";
    String expectedUrl = APP_CONFIG.baseUrl() + searchResultRelativePath;

    googleNavigationSteps.openSearchResultPage()
        .clickOnSearchResultLink(searchLinkText)
        .verify()
        .verifyOpenedPageUrl(expectedUrl);
  }
}
