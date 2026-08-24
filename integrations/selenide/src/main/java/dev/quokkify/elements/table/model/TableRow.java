package dev.quokkify.elements.table.model;

import java.util.Optional;

/**
 * A typed table row. Implementations may resolve cells lazily, so callers should not cache
 * underlying DOM elements across table updates.
 *
 * @param <C> typed column key
 */
public interface TableRow<C> {

  /** Returns the cell for a column, or empty when the column is not present in this row. */
  Optional<? extends TableCell<C>> cell(C column);

  /** Returns a required cell and fails when the row does not contain that column. */
  default TableCell<C> requiredCell(C column) {
    return cell(column).orElseThrow(() -> new TableCellNotFoundException(column));
  }
}
