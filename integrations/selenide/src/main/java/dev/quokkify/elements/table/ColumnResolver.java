package dev.quokkify.elements.table;

import java.util.List;

final class ColumnResolver {

  private ColumnResolver() {
  }

  static int indexOf(List<String> displayedHeaders, String header, String table) {
    List<String> trimmed = displayedHeaders.stream().map(String::trim).toList();
    int first = trimmed.indexOf(header);
    if (first < 0) {
      throw new TableColumnException(header, "not found", table, trimmed);
    }
    if (trimmed.lastIndexOf(header) != first) {
      throw new TableColumnException(header, "ambiguous", table, trimmed);
    }
    return first;
  }
}
