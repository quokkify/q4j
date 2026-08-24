package dev.quokkify.elements.table.model;

/** Thrown when a required cell is absent from a row. */
public class TableCellNotFoundException extends RuntimeException {

  public TableCellNotFoundException(Object column) {
    super("Cell for column '%s' was not found in the row".formatted(column));
  }
}
