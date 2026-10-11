package dev.quokkify.page.ux.component;

import dev.quokkify.elements.base.Component;
import dev.quokkify.elements.table.HorizontalTable;

import com.codeborne.selenide.SelenideElement;

public class KeyValueTable extends Component {

  public SelenideElement value(String label) {
    return HorizontalTable.of(getSelf()).value(label);
  }
}
