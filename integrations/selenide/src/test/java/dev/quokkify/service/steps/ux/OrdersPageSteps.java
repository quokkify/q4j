package dev.quokkify.service.steps.ux;

import dev.quokkify.model.PageSteps;
import dev.quokkify.page.ux.OrdersPage;
import dev.quokkify.service.verifications.ux.OrdersPageVerification;

import io.qameta.allure.Step;

public class OrdersPageSteps extends PageSteps<OrdersPageSteps, OrdersPageVerification, OrdersPage> {

  public OrdersPageSteps(OrdersPage page) {
    super.verification = new OrdersPageVerification(this, page);
    super.page = page;
  }

  @Step("Ship active order '{orderId}'")
  public OrdersPageSteps shipActiveOrder(String orderId) {
    page.activeOrders().orders().ship(orderId);
    return this;
  }

  @Step("Set active order '{orderId}' paid={paid}, note='{note}'")
  public OrdersPageSteps updateActiveOrder(String orderId, boolean paid, String note) {
    page.activeOrders().orders().paidCheckbox(orderId).setSelected(paid);
    page.activeOrders().orders().noteInput(orderId).setValue(note);
    return this;
  }

  @Step("Filter active orders by customer '{customer}'")
  public OrdersPageSteps filterActiveOrders(String customer) {
    page.activeOrders().filterByCustomer(customer);
    return this;
  }
}
