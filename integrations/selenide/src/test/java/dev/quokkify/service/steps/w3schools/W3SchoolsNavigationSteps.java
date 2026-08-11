package dev.quokkify.service.steps.w3schools;

import dev.quokkify.model.Navigation;
import dev.quokkify.page.w3school.HtmlHorizontalTablePage;
import dev.quokkify.page.w3school.HtmlTablesPage;

import io.qameta.allure.Step;

public class W3SchoolsNavigationSteps extends Navigation {

  public W3SchoolsNavigationSteps(String baseUrl) {
    super(baseUrl);
  }

  @Step("Open 'HTML Tables' page")
  public HtmlTablesPageSteps openHtmlTablePage() {
    return new HtmlTablesPageSteps(openPage(HtmlTablesPage.class));
  }

  @Step("Open 'HTML Horizontal Tables' page")
  public HtmlHorizontalTablePageSteps openHtmlHorizontalTablePage() {
    return new HtmlHorizontalTablePageSteps(openPage(HtmlHorizontalTablePage.class));
  }
}
