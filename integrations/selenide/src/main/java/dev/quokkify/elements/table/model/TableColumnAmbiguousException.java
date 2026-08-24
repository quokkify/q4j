package dev.quokkify.elements.table.model;

/** Thrown when a typed column maps to more than one displayed header. */
public class TableColumnAmbiguousException extends RuntimeException {

  public TableColumnAmbiguousException(Object column, String displayedHeader, Iterable<String> availableHeaders) {
    super("Column '%s' (displayed as '%s') is ambiguous; available headers: %s"
        .formatted(column, displayedHeader, availableHeaders));
  }
}
