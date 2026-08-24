package dev.quokkify.elements.table.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Framework-neutral contract for a DOM table model. It describes structure and lookup only;
 * sorting, filtering, pagination, selection, editing, virtualization, and loading are separate
 * capabilities and are intentionally not part of this contract.
 *
 * @param <C> typed column key
 */
public interface TableModel<C> {

  /** Returns headers exactly as displayed by the DOM, in DOM order. */
  List<String> displayedHeaders();

  /** Resolves a typed key to its displayed-header position. */
  default int columnIndex(C column, DisplayedHeaderResolver<C> resolver) {
    String displayedHeader = resolver.displayedHeader(column);
    List<String> headers = displayedHeaders();
    int index = headers.indexOf(displayedHeader);
    if (index < 0) {
      throw new TableColumnNotFoundException(column, displayedHeader, headers);
    }
    return index;
  }

  /** Returns the currently available rows without inventing missing rows. */
  List<? extends TableRow<C>> rows();

  /** Finds a currently available row. */
  default Optional<? extends TableRow<C>> row(Predicate<TableRow<C>> predicate) {
    return rows().stream().filter(predicate).findFirst();
  }

  /** Requires a currently available row; waiting policy belongs to the concrete adapter. */
  default TableRow<C> requiredRow(Predicate<TableRow<C>> predicate, String description) {
    return row(predicate).orElseThrow(() -> new TableRowNotFoundException(description));
  }
}
