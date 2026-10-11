package dev.quokkify.service.steps.ux;

import dev.quokkify.model.Navigation;
import dev.quokkify.page.ux.CustomerPage;
import dev.quokkify.page.ux.OrdersPage;

import io.qameta.allure.Step;

public class OrdersNavigationSteps extends Navigation {

  public OrdersNavigationSteps(String baseUrl) {
    super(baseUrl);
  }

  @Step("Open 'Orders' page")
  public OrdersPageSteps openOrdersPage() {
    return new OrdersPageSteps(openPage(OrdersPage.class));
  }

  @Step("Open 'Customer' page")
  public CustomerPage openCustomerPage() {
    return openPage(CustomerPage.class);
  }
}
