package dev.quokkify.elements.table.model;

/** Thrown when a required row is not available within the adapter's lookup policy. */
public class TableRowNotFoundException extends RuntimeException {

  public TableRowNotFoundException(String description) {
    super("Table row was not found: " + description);
  }
}
