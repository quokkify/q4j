package dev.quokkify.elements.table.model;

import java.util.Optional;

interface IndexedTableRow<C> extends TableRow<C> {

  Optional<String> cellText(int index);

  int columnIndex(C column);

  boolean isVisible();
}
