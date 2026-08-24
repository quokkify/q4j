package dev.quokkify.elements.table.model;

/** A typed cell value in a table row. */
public interface TableCell<C> {

  /** Column key represented by this cell. */
  C column();

  /** Current text value of the cell. */
  String text();
}
