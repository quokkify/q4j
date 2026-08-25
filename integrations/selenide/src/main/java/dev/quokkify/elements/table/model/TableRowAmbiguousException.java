package dev.quokkify.elements.table.model;

/** Raised when a query requiring one row matches more than one mounted row. */
public class TableRowAmbiguousException extends RuntimeException {

  public TableRowAmbiguousException(String description, int matchCount) {
    super("Expected one table row for " + description + ", but found " + matchCount);
  }
}
