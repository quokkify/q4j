package dev.quokkify.elements.table;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A row of a {@link TableSnapshot}. Cells are addressed by index or by displayed header text. Two rows are equal when
 * their cells are equal.
 */
public final class SnapshotRow {

  private final int index;
  private final List<SnapshotCell> cells;
  private final List<String> headers;
  private final String table;

  SnapshotRow(int index, List<SnapshotCell> cells, List<String> headers, String table) {
    this.index = index;
    this.cells = List.copyOf(Objects.requireNonNull(cells, "cells"));
    this.headers = List.copyOf(Objects.requireNonNull(headers, "headers"));
    this.table = Objects.requireNonNull(table, "table");
  }

  public List<SnapshotCell> cells() {
    return cells;
  }

  public SnapshotCell cell(int index) {
    if (index < 0 || index >= cells.size()) {
      throw new NoSuchElementException(
          "row " + this.index + " has " + cells.size() + " cells, no cell at index " + index + " in " + table);
    }
    return cells.get(index);
  }

  public SnapshotCell cell(String header) {
    int column = ColumnResolver.indexOf(headers, header, table);
    if (column >= cells.size()) {
      throw new NoSuchElementException(
          "row " + index + " has " + cells.size() + " cells, no cell for header \"" + header + "\" in " + table);
    }
    return cells.get(column);
  }

  SnapshotCell cellOrNull(int column) {
    return column < cells.size() ? cells.get(column) : null;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof SnapshotRow row && cells.equals(row.cells);
  }

  @Override
  public int hashCode() {
    return cells.hashCode();
  }

  @Override
  public String toString() {
    return "SnapshotRow" + cells;
  }
}
