package dev.quokkify.elements.table.model;

/** Lazy reference to a cell addressed by zero-based row and column indexes. */
public interface TableCellRef<C> {

  int rowIndex();

  int columnIndex();

  String text();
}
