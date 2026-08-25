package dev.quokkify.elements.table.model;

/** Raised when an edit capability is requested from a read-only table cell. */
public final class UnsupportedTableEditException extends UnsupportedOperationException {

  public UnsupportedTableEditException(String cellDescription) {
    super("Table cell is read-only and has no explicit editable control: " + cellDescription);
  }
}
