package dev.quokkify.page.ux;

import dev.quokkify.annotation.PageUrl;
import dev.quokkify.impl.Page;
import dev.quokkify.page.ux.component.KeyValueTable;
import dev.quokkify.page.ux.component.OrdersTable;

import org.openqa.selenium.support.FindBy;

@PageUrl("/page-object-ux/customer.html")
public class CustomerPage implements Page {

  @FindBy(id = "profile")
  private KeyValueTable profile;

  @FindBy(css = "#customer-orders table")
  private OrdersTable orders;

  public KeyValueTable profile() {
    return profile;
  }

  public OrdersTable orders() {
    return orders;
  }
}
