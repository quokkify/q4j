package dev.quokkify.elements.table.model;

import java.util.List;

/** Lazy reference to a zero-based logical table column. */
public interface TableColumnRef<C> {

  int index();

  List<? extends TableCellRef<C>> cells();
}
