package dev.quokkify.elements.table;

import java.util.List;

public class TableColumnException extends RuntimeException {

  public TableColumnException(String header, String reason, String table, List<String> displayedHeaders) {
    super("Column \"" + header + "\" " + reason + " in " + table + "; displayed headers: " + displayedHeaders);
  }
}
