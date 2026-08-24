package dev.quokkify.elements.table.model;

/** Thrown when a typed column has no matching displayed header in the table. */
public class TableColumnNotFoundException extends RuntimeException {

  public TableColumnNotFoundException(Object column, String displayedHeader, Iterable<String> availableHeaders) {
    super("Column '%s' (displayed as '%s') was not found; available headers: %s"
        .formatted(column, displayedHeader, availableHeaders));
  }
}
