package dev.quokkify.elements.table.model;

import java.util.Optional;

import com.codeborne.selenide.SelenideElement;

interface IndexedTableRow<C> extends TableRow<C> {

  Optional<String> cellText(int index);

  SelenideElement cellElement(int index);

  int columnIndex(C column);

  boolean isVisible();
}
