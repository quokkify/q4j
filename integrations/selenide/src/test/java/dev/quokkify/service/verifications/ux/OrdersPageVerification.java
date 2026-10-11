package dev.quokkify.service.verifications.ux;

import dev.quokkify.model.Verification;
import dev.quokkify.page.ux.OrdersPage;
import dev.quokkify.service.steps.ux.OrdersPageSteps;

import io.qameta.allure.Step;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Condition.visible;

public class OrdersPageVerification extends Verification<OrdersPageSteps, OrdersPageVerification, OrdersPage> {

  public OrdersPageVerification(OrdersPageSteps steps, OrdersPage page) {
    super(steps, page);
  }

  @Step("Verify active order '{orderId}' has status '{status}'")
  public OrdersPageVerification activeOrderHasStatus(String orderId, String status) {
    page.activeOrders().orders().order(orderId).cell("Status").shouldHave(exactText(status), getTimeout());
    return this;
  }

  @Step("Verify active order '{orderId}' belongs to '{customer}' with qty {qty}")
  public OrdersPageVerification activeOrderHas(String orderId, String customer, String qty) {
    var order = page.activeOrders().orders().order(orderId);
    order.cell("Customer").shouldHave(exactText(customer), getTimeout());
    order.cell("Qty").shouldHave(exactText(qty), getTimeout());
    return this;
  }

  @Step("Verify active order '{orderId}' is paid with note '{note}'")
  public OrdersPageVerification activeOrderIsPaidWithNote(String orderId, String note) {
    page.activeOrders().orders().paidCheckbox(orderId).shouldBe(checked, getTimeout());
    page.activeOrders().orders().noteInput(orderId).shouldHave(exactValue(note), getTimeout());
    return this;
  }

  @Step("Verify active order IDs are {ids}")
  public OrdersPageVerification activeOrderIds(String... ids) {
    page.activeOrders().orders().ids().shouldHave(exactTexts(ids), getTimeout());
    return this;
  }

  @Step("Verify visible active orders are {ids}")
  public OrdersPageVerification visibleActiveOrderIds(String... ids) {
    page.activeOrders().orders().table().rows().filterBy(visible).shouldHave(size(ids.length), getTimeout());
    page.activeOrders().orders().ids().filterBy(visible).shouldHave(exactTexts(ids), getTimeout());
    return this;
  }

  @Step("Verify archived order IDs are {ids}")
  public OrdersPageVerification archivedOrderIds(String... ids) {
    page.archivedOrders().orders().ids().shouldHave(exactTexts(ids), getTimeout());
    return this;
  }

  @Step("Verify inventory stock of '{product}' is {stock}")
  public OrdersPageVerification inventoryStock(String product, String stock) {
    page.inventory().table().row("Product", product).cell("Stock").shouldHave(exactText(stock), getTimeout());
    return this;
  }

  @Step("Verify summary '{label}' is {value}")
  public OrdersPageVerification summaryValue(String label, String value) {
    page.summary().value(label).shouldHave(exactText(value), getTimeout());
    return this;
  }
}
