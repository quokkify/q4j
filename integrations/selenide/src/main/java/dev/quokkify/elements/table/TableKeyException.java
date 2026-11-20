package dev.quokkify.elements.table;

/**
 * A key column of a {@link TableSnapshot} has no cell in a row or repeats a key. Package-private so table conditions
 * can treat it as "not ready yet" without catching every {@link IllegalStateException} thrown by caller code.
 */
final class TableKeyException extends IllegalStateException {

  TableKeyException(String message) {
    super(message);
  }
}
