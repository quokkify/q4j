package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.codeborne.selenide.SelenideElement;

/**
 * Additive Selenide query layer over the framework-neutral table model.
 *
 * <p>All indexes are zero-based. Returned references retain only indexes or typed keys and resolve
 * the current DOM on every operation.</p>
 */
public final class SelenideTableQuery<C> {

  private final SelenideDomTableModel<C> model;

  /** Creates a query for a Selenide DOM model. */
  public SelenideTableQuery(SelenideDomTableModel<C> model) {
    this.model = Objects.requireNonNull(model, "model");
  }

  /** Creates a query for any typed key using an explicit displayed-header resolver. */
  public static <C> SelenideTableQuery<C> of(SelenideElement table, TableDomAdapter adapter,
                                             Function<C, String> displayedHeader) {
    SelenideDomTableModel<C> model = SelenideDomTableModel.of(table, adapter,
        DisplayedHeaderResolver.requiringNonNull(displayedHeader));
    return new SelenideTableQuery<>(model);
  }

  /** Returns every currently mounted row, including rows hidden by CSS. */
  public List<TableQueryRow<C>> mountedRows() {
    return new AbstractList<>() {
      @Override
      public TableQueryRow<C> get(int index) {
        int rowCount = model.rows().size();
        if (index < 0 || index >= rowCount) {
          throw new IndexOutOfBoundsException(index);
        }
        return rowReference(index);
      }

      @Override
      public int size() {
        return model.rows().size();
      }
    };
  }

  /** Returns currently mounted rows that are displayed. */
  public List<TableQueryRow<C>> visibleRows() {
    return mountedRows().stream().filter(TableQueryRow::isVisible).toList();
  }

  /** Returns a required zero-based mounted row. */
  public TableQueryRow<C> row(int index) {
    return mountedRows().get(index);
  }

  /** Returns the first mounted row. */
  public TableQueryRow<C> firstRow() {
    return row(0);
  }

  /** Returns the last mounted row. */
  public TableQueryRow<C> lastRow() {
    return row(mountedRows().size() - 1);
  }

  /** Returns a required cell by zero-based mounted-row and data-cell indexes. */
  public TableCellRef<C> cell(int rowIndex, int columnIndex) {
    return row(rowIndex).cell(columnIndex);
  }

  /** Returns a typed cell, or empty when the row does not contain the column. */
  public Optional<? extends TypedTableCellRef<C>> cell(int rowIndex, C column) {
    return row(rowIndex).cell(column);
  }

  /** Returns a lazy zero-based logical column. */
  public TableColumnRef<C> column(int index) {
    validateColumnIndex(index);
    return new IndexedColumnReference(index);
  }

  /** Returns a lazy typed logical column. */
  public TypedTableColumnRef<C> column(C column) {
    Objects.requireNonNull(column, "column");
    model.typedColumnIndex(column);
    return new TypedColumnReference(column);
  }

  /** Finds the first currently mounted matching row. */
  public Optional<TableQueryRow<C>> findRow(RowCondition<C> condition) {
    Objects.requireNonNull(condition, "condition");
    return mountedRows().stream().filter(condition::test).findFirst();
  }

  /** Finds every currently mounted matching row in DOM order. */
  public List<TableQueryRow<C>> findRows(RowCondition<C> condition) {
    Objects.requireNonNull(condition, "condition");
    return mountedRows().stream().filter(condition::test).toList();
  }

  /** Requires the first currently mounted matching row. */
  public TableQueryRow<C> row(RowCondition<C> condition) {
    return requiredRow(condition);
  }

  /** Requires the first matching row using the model's native Selenide wait. */
  public TableQueryRow<C> row(RowCondition<C> condition, Duration timeout) {
    return requiredRow(condition, timeout);
  }

  /** Requires the first currently mounted matching row. */
  public TableQueryRow<C> requiredRow(RowCondition<C> condition) {
    return findRow(condition).orElseThrow(() -> new TableRowNotFoundException("query condition"));
  }

  /** Requires the first matching row using the model's native Selenide wait. */
  public TableQueryRow<C> requiredRow(RowCondition<C> condition, Duration timeout) {
    Objects.requireNonNull(condition, "condition");
    Objects.requireNonNull(timeout, "timeout");
    model.requiredIndexedRow((index, candidate) -> condition.test(conditionRow(index, candidate)),
        "query condition", timeout);
    return requiredRow(condition);
  }

  /** Requires exactly one currently mounted matching row. */
  public TableQueryRow<C> uniqueRow(RowCondition<C> condition) {
    Objects.requireNonNull(condition, "condition");
    Optional<TableQueryRow<C>> first = Optional.empty();
    for (TableQueryRow<C> candidate : mountedRows()) {
      if (condition.test(candidate)) {
        if (first.isPresent()) {
          throw new TableRowAmbiguousException("query condition", 2);
        }
        first = Optional.of(candidate);
      }
    }
    return first.orElseThrow(() -> new TableRowNotFoundException("unique query condition"));
  }

  /** Waits for a match and then requires exactly one currently mounted matching row. */
  public TableQueryRow<C> uniqueRow(RowCondition<C> condition, Duration timeout) {
    requiredRow(condition, timeout);
    return uniqueRow(condition);
  }

  private TableQueryRow<C> rowReference(int index) {
    return new TableQueryRow<>(index, () -> model.rows().get(index));
  }

  private TableQueryRow<C> conditionRow(int index, TableRow<C> candidate) {
    return new TableQueryRow<>(index, () -> candidate);
  }

  private void validateColumnIndex(int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException(index);
    }
    if (model.isHorizontal()) {
      if (index >= model.rows().size()) {
        throw new IndexOutOfBoundsException(index);
      }
    } else if (!model.displayedHeaders().isEmpty()) {
      if (index >= model.displayedHeaders().size()) {
        throw new IndexOutOfBoundsException(index);
      }
    } else if (!model.hasDataCell(index)) {
      throw new IndexOutOfBoundsException(index);
    }
  }

  private final class IndexedColumnReference implements TableColumnRef<C> {
    private final int index;

    private IndexedColumnReference(int index) {
      this.index = index;
    }

    @Override
    public int index() {
      return index;
    }

    @Override
    public List<? extends TableCellRef<C>> cells() {
      if (model.isHorizontal()) {
        return List.of(row(index).cell(0));
      }
      List<TableCellRef<C>> cells = new ArrayList<>();
      for (TableQueryRow<C> current : mountedRows()) {
        try {
          cells.add(current.cell(index));
        } catch (IndexOutOfBoundsException ignored) {
          // A mounted row may legitimately omit a trailing cell.
        }
      }
      return List.copyOf(cells);
    }
  }

  private final class TypedColumnReference implements TypedTableColumnRef<C> {
    private final C column;

    private TypedColumnReference(C column) {
      this.column = column;
    }

    @Override
    public C column() {
      return column;
    }

    @Override
    public int index() {
      return model.typedColumnIndex(column);
    }

    @Override
    public List<? extends TypedTableCellRef<C>> cells() {
      return mountedRows().stream().map(current -> current.cell(column))
          .flatMap(Optional::stream).toList();
    }
  }
}
