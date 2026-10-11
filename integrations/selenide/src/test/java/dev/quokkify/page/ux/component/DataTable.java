package dev.quokkify.page.ux.component;

import dev.quokkify.elements.base.Component;
import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableLayout;

public class DataTable extends Component {

  protected TableLayout layout() {
    return TableLayout.html();
  }

  public Table table() {
    return Table.of(getSelf(), layout());
  }
}
