package dev.quokkify.elements.table.model;

import java.util.List;

/** Lazy logical column reference that also retains its typed column key. */
public interface TypedTableColumnRef<C> extends TableColumnRef<C> {

  C column();

  @Override
  List<? extends TypedTableCellRef<C>> cells();
}
