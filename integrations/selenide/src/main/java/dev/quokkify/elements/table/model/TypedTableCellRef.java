package dev.quokkify.elements.table.model;

/** Lazy indexed cell reference that also retains its typed column key. */
public interface TypedTableCellRef<C> extends TableCellRef<C>, TableCell<C> {
}
