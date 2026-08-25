package dev.quokkify.elements.table.model;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Lazy query row addressed by its zero-based mounted-row index. */
public final class TableQueryRow<C> {

  private final int index;
  private final Supplier<? extends TableRow<C>> row;

  TableQueryRow(int index, Supplier<? extends TableRow<C>> row) {
    this.index = index;
    this.row = Objects.requireNonNull(row, "row");
  }

  /** Returns the zero-based mounted-row index captured by this reference. */
  public int index() {
    return index;
  }

  /** Returns whether the current DOM row is displayed. */
  public boolean isVisible() {
    return indexedRow().isVisible();
  }

  /** Returns a required cell addressed by its zero-based index. */
  public TableCellRef<C> cell(int columnIndex) {
    indexedRow().cellText(columnIndex).orElseThrow(() -> new IndexOutOfBoundsException(columnIndex));
    return new IndexedCellReference(columnIndex);
  }

  /** Returns a typed cell, or empty when this mounted row does not contain the column. */
  public Optional<? extends TypedTableCellRef<C>> cell(C column) {
    Objects.requireNonNull(column, "column");
    return row.get().cell(column).map(cell -> new TypedCellReference(column));
  }

  /** Returns a required typed cell. */
  public TypedTableCellRef<C> requiredCell(C column) {
    return cell(column).orElseThrow(() -> new TableCellNotFoundException(column));
  }

  @SuppressWarnings("unchecked")
  private IndexedTableRow<C> indexedRow() {
    TableRow<C> current = row.get();
    if (!(current instanceof IndexedTableRow<?>)) {
      throw new IllegalStateException("Query rows require an indexed Selenide table model");
    }
    return (IndexedTableRow<C>) current;
  }

  private final class IndexedCellReference implements TableCellRef<C> {
    private final int columnIndex;

    private IndexedCellReference(int columnIndex) {
      this.columnIndex = columnIndex;
    }

    @Override
    public int rowIndex() {
      return index;
    }

    @Override
    public int columnIndex() {
      return columnIndex;
    }

    @Override
    public String text() {
      return indexedRow().cellText(columnIndex)
          .orElseThrow(() -> new IndexOutOfBoundsException(columnIndex));
    }
  }

  private final class TypedCellReference implements TypedTableCellRef<C> {
    private final C column;

    private TypedCellReference(C column) {
      this.column = column;
    }

    @Override
    public int rowIndex() {
      return index;
    }

    @Override
    public int columnIndex() {
      return indexedRow().columnIndex(column);
    }

    @Override
    public C column() {
      return column;
    }

    @Override
    public String text() {
      return row.get().requiredCell(column).text();
    }
  }
}
