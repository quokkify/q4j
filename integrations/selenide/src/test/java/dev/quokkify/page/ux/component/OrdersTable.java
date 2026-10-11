package dev.quokkify.page.ux.component;

import dev.quokkify.elements.table.TableRow;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class OrdersTable extends DataTable {

  public TableRow order(String id) {
    return table().row("ID", id);
  }

  public ElementsCollection ids() {
    return table().column("ID");
  }

  public SelenideElement paidCheckbox(String id) {
    return order(id).cell("Paid").$("input[type=checkbox]");
  }

  public SelenideElement noteInput(String id) {
    return order(id).cell("Note").$("input");
  }

  public void ship(String id) {
    order(id).cell("Action").$("button").click();
  }
}
