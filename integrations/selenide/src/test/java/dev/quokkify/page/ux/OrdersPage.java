package dev.quokkify.page.ux;

import java.util.List;

import dev.quokkify.annotation.PageUrl;
import dev.quokkify.elements.base.Component;
import dev.quokkify.impl.Page;
import dev.quokkify.page.ux.component.InventoryGrid;
import dev.quokkify.page.ux.component.KeyValueTable;
import dev.quokkify.page.ux.component.OrdersTable;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.support.FindBy;

@PageUrl("/page-object-ux/orders.html")
public class OrdersPage implements Page {

  @FindBy(id = "active-orders")
  private OrdersPanel activeOrders;

  @FindBy(id = "archived-orders")
  private OrdersPanel archivedOrders;

  @FindBy(css = ".orders-panel")
  private List<OrdersPanel> panels;

  @FindBy(id = "inventory")
  private InventoryGrid inventory;

  @FindBy(id = "summary")
  private KeyValueTable summary;

  public OrdersPanel activeOrders() {
    return activeOrders;
  }

  public OrdersPanel archivedOrders() {
    return archivedOrders;
  }

  public List<OrdersPanel> panels() {
    return panels;
  }

  public InventoryGrid inventory() {
    return inventory;
  }

  public KeyValueTable summary() {
    return summary;
  }

  public static class OrdersPanel extends Component {

    @FindBy(css = ".panel-title")
    private SelenideElement title;

    @FindBy(css = ".filter")
    private SelenideElement filter;

    @FindBy(css = "table.orders")
    private OrdersTable orders;

    public SelenideElement title() {
      return title;
    }

    public void filterByCustomer(String customer) {
      filter.setValue(customer);
    }

    public OrdersTable orders() {
      return orders;
    }
  }
}
