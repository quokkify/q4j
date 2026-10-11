package dev.quokkify.test;

import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableColumnException;
import dev.quokkify.page.ux.CustomerPage;
import dev.quokkify.page.ux.OrdersPage;
import dev.quokkify.service.steps.ux.OrdersNavigationSteps;

import com.codeborne.selenide.Selenide;
import org.openqa.selenium.support.FindBy;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.exactText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OrdersTablePageObjectTest extends BaseTest {

  private final OrdersNavigationSteps ordersNavigation = new OrdersNavigationSteps(APP_CONFIG.baseUrl());

  @Test(description = "Table inside a @FindBy Component reads a delayed row by column value")
  public void readsDelayedRowThroughNestedComponent() {
    ordersNavigation.openOrdersPage()
        .verify()
        .activeOrderHas("A-2", "Ernst Handel", "12")
        .activeOrderHasStatus("A-2", "New")
        .activeOrderIds("A-1", "A-2", "A-3");
  }

  @Test(description = "Cell button action updates the row and a horizontal summary table")
  public void shipsOrderFromCellButton() {
    ordersNavigation.openOrdersPage()
        .verify()
        .summaryValue("Shipped", "0")
        .backToSteps()
        .shipActiveOrder("A-1")
        .verify()
        .activeOrderHasStatus("A-1", "Shipped")
        .summaryValue("Shipped", "1");
  }

  @Test(description = "Checkbox and input inside cells are edited through the row")
  public void editsControlsInsideCells() {
    ordersNavigation.openOrdersPage()
        .updateActiveOrder("A-1", true, "fragile")
        .verify()
        .activeOrderIsPaidWithNote("A-1", "fragile");
  }

  @Test(description = "One Component class backs two panels whose tables stay isolated")
  public void sameComponentClassKeepsTablesIsolated() {
    ordersNavigation.openOrdersPage()
        .verify()
        .activeOrderIds("A-1", "A-2", "A-3")
        .archivedOrderIds("Z-9");
    OrdersPage page = Selenide.page(OrdersPage.class);

    assertThat(page.panels()).extracting(panel -> panel.title().getText())
        .containsExactly("Active orders", "Archived orders");
    page.panels().get(1).orders().table().rows("Customer", "Ernst Handel").shouldHave(size(1));
  }

  @Test(description = "Filter input hides rows; visible rows are filtered explicitly")
  public void filterHidesRows() {
    ordersNavigation.openOrdersPage()
        .verify()
        .activeOrderIds("A-1", "A-2", "A-3")
        .backToSteps()
        .filterActiveOrders("Ernst")
        .verify()
        .visibleActiveOrderIds("A-2");
  }

  @Test(description = "ARIA div grid is a @FindBy component with an overridden layout")
  public void readsAriaGridFromFindByElement() {
    ordersNavigation.openOrdersPage()
        .verify()
        .inventoryStock("Chang", "0")
        .inventoryStock("Chai", "39");
  }

  @Test(description = "Unknown header names the displayed headers in the failure")
  public void unknownHeaderReportsDisplayedHeaders() {
    ordersNavigation.openOrdersPage();
    OrdersPage page = Selenide.page(OrdersPage.class);
    page.activeOrders().orders().ids().shouldHave(exactTexts("A-1", "A-2", "A-3"));

    assertThatThrownBy(() -> page.activeOrders().orders().order("A-1").cell("Shipping"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("Column \"Shipping\" not found")
        .hasMessageContaining("[ID, Customer, Status, Qty, Paid, Note, Action]");
  }

  @Test(description = "The same OrdersTable component is reused on another page with another locator")
  public void reusesOrdersTableOnAnotherPage() {
    CustomerPage page = ordersNavigation.openCustomerPage();

    page.profile().value("Customer").shouldHave(exactText("Ernst Handel"));
    page.orders().ids().shouldHave(exactTexts("A-2", "Z-9"));
    page.orders().order("Z-9").cell("Status").shouldHave(exactText("Delivered"));
    page.orders().paidCheckbox("A-2").shouldBe(checked);
    page.orders().table().rows("Customer", "Ernst Handel").shouldHave(size(2));
  }

  @Test(description = "Page factory leaves a plain @FindBy Table field null, so tables are wrapped in a Component")
  public void findByTableFieldIsLeftNull() {
    ordersNavigation.openOrdersPage();

    PageWithTableField page = Selenide.page(PageWithTableField.class);

    assertThat(page.orders).as("@FindBy Table field after Selenide.page(...)").isNull();
  }

  public static class PageWithTableField {

    @FindBy(css = "#active-orders table.orders")
    private Table orders;
  }
}
