package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Lazy query row addressed by its zero-based mounted-row index. */
public final class TableQueryRow<C> {

  private final int index;
  private final Supplier<? extends TableRow<C>> row;
  private final SelenideDomTableModel<C> model;

  TableQueryRow(int index, Supplier<? extends TableRow<C>> row, SelenideDomTableModel<C> model) {
    this.index = index;
    this.row = Objects.requireNonNull(row, "row");
    this.model = Objects.requireNonNull(model, "model");
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

  /** Waits for a Selenide-native assertion against this lazily re-resolved row. */
  public TableQueryRow<C> shouldHave(RowAssertion<C> assertion) {
    model.assertRow(index, assertion);
    return this;
  }

  /** Waits for a Selenide-native assertion against this lazily re-resolved row. */
  public TableQueryRow<C> shouldHave(RowAssertion<C> assertion, Duration timeout) {
    model.assertRow(index, assertion, Objects.requireNonNull(timeout, "timeout"));
    return this;
  }

  @SuppressWarnings("unchecked")
  private IndexedTableRow<C> indexedRow() {
    TableRow<C> current = row.get();
    if (!(current instanceof IndexedTableRow<?>)) {
      throw new IllegalStateException("Query rows require an indexed Selenide table model");
    }
    return (IndexedTableRow<C>) current;
  }

  private final class IndexedCellReference extends ElementTableCellRef<C> {
    private IndexedCellReference(int columnIndex) {
      super(index, columnIndex,
          () -> indexedRow().cellElement(columnIndex),
          "row index " + index + ", column index " + columnIndex);
    }

    @Override
    public String text() {
      return indexedRow().cellText(columnIndex())
          .orElseThrow(() -> new IndexOutOfBoundsException(columnIndex()));
    }
  }

  private final class TypedCellReference extends ElementTableCellRef<C>
      implements TypedTableCellRef<C> {
    private final C column;

    private TypedCellReference(C column) {
      super(TableQueryRow.this.index, TableQueryRow.this.indexedRow().columnIndex(column),
          () -> indexedRow().cellElement(indexedRow().columnIndex(column)),
          "row index " + TableQueryRow.this.index + ", column key " + column
              + ", displayed header " + model.displayedHeaderResolver().displayedHeader(column));
      this.column = column;
    }

    @Override
    public int columnIndex() {
      return indexedRow().columnIndex(column);
    }

    @Override
    public String text() {
      return row.get().requiredCell(column).text();
    }

    @Override
    public C column() {
      return column;
    }
  }
}
