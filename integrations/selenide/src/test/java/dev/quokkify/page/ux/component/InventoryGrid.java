package dev.quokkify.page.ux.component;

import dev.quokkify.elements.table.TableLayout;

public class InventoryGrid extends DataTable {

  @Override
  protected TableLayout layout() {
    return TableLayout.aria();
  }
}
