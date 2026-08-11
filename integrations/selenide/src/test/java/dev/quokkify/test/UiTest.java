package dev.quokkify.test;

import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

public class UiTest extends BaseTest {

  @TmsLink("UI_ID_1")
  @Test(description = "Verify 'BLOCK', 'INPUT', 'BUTTON', 'COLLECTION'")
  public void testVerifyBlockInputButtonCollection() {
    googleNavigationSteps.openSearchResultPage()
        .verify()
        .verifySearchResultsExist();
  }
}
