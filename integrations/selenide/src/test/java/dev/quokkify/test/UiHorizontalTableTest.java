package dev.quokkify.test;

import dev.quokkify.page.w3school.HtmlHorizontalTablePage;

import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

public class UiHorizontalTableTest extends BaseTest {

  @TmsLink("UI_ID_5")
  @Test(description = "Verify 'HORIZONTAL TABLE'")
  public void testTable() {
    Person person = new Person("Bill Gates", "555 77 854", "555 77 855");

    w3SchoolsNavigationSteps.openHtmlHorizontalTablePage()
        .acceptTerms()
        .verify()
        .verifyTableRow(HtmlHorizontalTablePage.Header.NAME, person.name())
        .verifyTableRow(HtmlHorizontalTablePage.Header.TELEPHONE_1, person.telephone1())
        .verifyTableRow(HtmlHorizontalTablePage.Header.TELEPHONE_2, person.telephone2());
  }

  private record Person(String name, String telephone1, String telephone2) {

  }
}
