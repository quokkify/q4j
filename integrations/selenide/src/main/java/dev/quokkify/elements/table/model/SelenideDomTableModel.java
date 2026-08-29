package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.impl.WebElementWrapper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

/**
 * Selenide-backed implementation of the neutral table model contract.
 *
 * <p>The table and row elements remain Selenide's lazy elements. Header and cell text is read when
 * the corresponding operation is invoked, which keeps returned rows usable after a DOM refresh.</p>
 *
 * @param <C> typed column key
 */
public final class SelenideDomTableModel<C> implements TableModel<C> {

  private final SelenideElement table;
  private final TableDomAdapter adapter;
  private final DisplayedHeaderResolver<C> resolver;

  /** Creates a model backed by a public per-table DOM adapter. */
  public SelenideDomTableModel(SelenideElement table, TableDomAdapter adapter,
                               DisplayedHeaderResolver<C> resolver) {
    this.table = Objects.requireNonNull(table, "table");
    this.adapter = Objects.requireNonNull(adapter, "adapter");
    this.resolver = Objects.requireNonNull(resolver, "resolver");
  }

  /** Creates a model backed by a public per-table DOM adapter. */
  public static <C> SelenideDomTableModel<C> of(SelenideElement table, TableDomAdapter adapter,
                                                DisplayedHeaderResolver<C> resolver) {
    return new SelenideDomTableModel<>(table, adapter, resolver);
  }

  /** Compatibility constructor for the legacy layout enum. */
  public SelenideDomTableModel(SelenideElement table, DomTableLayout layout,
                               DisplayedHeaderResolver<C> resolver) {
    this(table, adapterFor(layout), resolver);
  }

  @Override
  public List<String> displayedHeaders() {
    if (adapter.headerLocator() instanceof TableHeaderRowLocator headers) {
      return table.find(headers.headerRowLocator()).findAll(headers.headerCellLocator()).texts();
    }
    if (adapter.headerLocator() instanceof RowHeaderCellLocator headers) {
      return rowsElements().stream()
          .map(row -> row.find(headers.headerCellLocator()).text())
          .toList();
    }
    return List.of();
  }

  @Override
  public List<? extends TableRow<C>> rows() {
    if (!table.exists()) {
      return List.of();
    }
    return new AbstractList<>() {
      @Override
      public TableRow<C> get(int index) {
        if (index < 0 || index >= size()) {
          throw new IndexOutOfBoundsException(index);
        }
        return rowAt(index);
      }

      @Override
      public int size() {
        return rowsElements().size();
      }
    };
  }

  @Override
  public TableRow<C> requiredRow(Predicate<TableRow<C>> predicate, String description, Duration timeout) {
    return rowAt(requiredRowIndex((index, row) -> predicate.test(row), description, timeout));
  }

  int requiredRowIndex(BiPredicate<Integer, TableRow<C>> predicate,
                       String description, Duration timeout) {
    MatchingTableCondition condition = new MatchingTableCondition(predicate, description);
    try {
      table.shouldBe(condition, timeout);
      return condition.matchedIndex();
    } catch (UIAssertionError error) {
      TableRowNotFoundException failure = new TableRowNotFoundException(
          description + "; timeout=" + timeout);
      failure.initCause(error);
      throw failure;
    }
  }

  private ElementsCollection rowsElements() {
    return table.findAll(adapter.mountedDataRowLocator());
  }

  private SelenideRow rowAt(int index) {
    return new SelenideRow(() -> rowsElements().get(index));
  }

  int typedColumnIndex(C column) {
    return columnIndex(column, resolver);
  }

  boolean isHorizontal() {
    return adapter.headerLocator() instanceof RowHeaderCellLocator;
  }

  boolean hasDataCell(int cellIndex) {
    return rowsElements().stream()
        .anyMatch(row -> row.findAll(adapter.dataCellLocator()).size() > cellIndex);
  }

  DisplayedHeaderResolver<C> displayedHeaderResolver() {
    return resolver;
  }

  SelenideElement rowElement(int rowIndex) {
    return rowsElements().get(rowIndex);
  }

  SelenideElement cellElement(int rowIndex, int columnIndex) {
    return rowElement(rowIndex).findAll(adapter.dataCellLocator()).get(columnIndex);
  }

  SelenideElement typedCellElement(int rowIndex, C column) {
    int columnIndex = adapter.headerLocator() instanceof RowHeaderCellLocator ? 0
        : typedColumnIndex(column);
    return cellElement(rowIndex, columnIndex);
  }

  void assertTable(TableAssertion<C> assertion) {
    table.shouldHave(new SnapshotTableCondition(assertion));
  }

  void assertTable(TableAssertion<C> assertion, Duration timeout) {
    table.shouldHave(new SnapshotTableCondition(assertion), timeout);
  }

  void assertRow(int rowIndex, RowAssertion<C> assertion) {
    assertTable(rowAssertion(rowIndex, assertion));
  }

  void assertRow(int rowIndex, RowAssertion<C> assertion, Duration timeout) {
    assertTable(rowAssertion(rowIndex, assertion), timeout);
  }

  void assertColumn(int columnIndex, ColumnAssertion assertion) {
    assertTable(columnAssertion("index=" + columnIndex, columnIndex, assertion));
  }

  void assertColumn(int columnIndex, ColumnAssertion assertion, Duration timeout) {
    assertTable(columnAssertion("index=" + columnIndex, columnIndex, assertion), timeout);
  }

  void assertColumn(C column, ColumnAssertion assertion) {
    assertTable(typedColumnAssertion(column, assertion));
  }

  void assertColumn(C column, ColumnAssertion assertion, Duration timeout) {
    assertTable(typedColumnAssertion(column, assertion), timeout);
  }

  private TableAssertion<C> rowAssertion(int rowIndex, RowAssertion<C> assertion) {
    RowAssertion<C> required = Objects.requireNonNull(assertion, "assertion");
    return new TableAssertion<>() {
      @Override
      public String description() {
        return "row index " + rowIndex + " " + required.description();
      }

      @Override
      public boolean test(Driver driver, TableAssertionContext<C> context) {
        return rowIndex >= 0 && rowIndex < context.rowCount()
            && required.test(driver, context.row(rowIndex));
      }
    };
  }

  private TableAssertion<C> columnAssertion(String address, int columnIndex,
                                             ColumnAssertion assertion) {
    ColumnAssertion required = Objects.requireNonNull(assertion, "assertion");
    return new TableAssertion<>() {
      @Override
      public String description() {
        return "column " + address + " " + required.description();
      }

      @Override
      public boolean test(Driver driver, TableAssertionContext<C> context) {
        return required.test(context.columnValues(columnIndex));
      }
    };
  }

  private TableAssertion<C> typedColumnAssertion(C column, ColumnAssertion assertion) {
    String displayed = resolver.displayedHeader(Objects.requireNonNull(column, "column"));
    ColumnAssertion required = Objects.requireNonNull(assertion, "assertion");
    return new TableAssertion<>() {
      @Override
      public String description() {
        return "column key=" + column + ", header=" + displayed + " " + required.description();
      }

      @Override
      public boolean test(Driver driver, TableAssertionContext<C> context) {
        int index = context.columnIndex(column);
        return index >= 0 && required.test(context.columnValues(index));
      }
    };
  }

  private final class SnapshotTableCondition extends WebElementCondition {
    private final TableAssertion<C> assertion;

    private SnapshotTableCondition(TableAssertion<C> assertion) {
      super(Objects.requireNonNull(assertion, "assertion").description() + " on " + table);
      this.assertion = assertion;
    }

    @Override
    public CheckResult check(Driver driver, WebElement tableElement) {
      try {
        TableAssertionContext<C> context = new TableAssertionContext<>(tableElement, adapter, resolver);
        return assertion.test(driver, context)
            ? CheckResult.accepted(context.diagnostics())
            : CheckResult.rejected(assertion.description(), context.diagnostics());
      } catch (NoSuchElementException | StaleElementReferenceException | IndexOutOfBoundsException error) {
        return CheckResult.rejected(error.toString(), "table changed while checking assertion");
      }
    }
  }

  private final class MatchingTableCondition extends WebElementCondition {
    private final BiPredicate<Integer, TableRow<C>> predicate;
    private int matchedIndex = -1;

    private MatchingTableCondition(BiPredicate<Integer, TableRow<C>> predicate,
                                   String description) {
      super(description);
      this.predicate = predicate;
    }

    @Override
    public CheckResult check(Driver driver, WebElement tableElement) {
      try {
        matchedIndex = -1;
        List<WebElement> rowElements = tableElement.findElements(adapter.mountedDataRowLocator());
        for (int index = 0; index < rowElements.size(); index++) {
          WebElement rowElement = rowElements.get(index);
          TableRow<C> candidate = new SelenideRow(
              () -> WebElementWrapper.wrap(driver, rowElement, "table row candidate"));
          if (predicate.test(index, candidate)) {
            matchedIndex = index;
            return CheckResult.accepted("matched row");
          }
        }
        return CheckResult.rejected("row does not match", "table has no matching row yet");
      } catch (NoSuchElementException | StaleElementReferenceException error) {
        return CheckResult.rejected(error.toString(), "table changed while checking rows");
      }
    }

    private int matchedIndex() {
      if (matchedIndex < 0) {
        throw new NoSuchElementException("table has no matching data row");
      }
      return matchedIndex;
    }
  }

  private final class SelenideRow implements IndexedTableRow<C> {
    private final Supplier<SelenideElement> row;

    private SelenideRow(Supplier<SelenideElement> row) {
      this.row = row;
    }

    @Override
    public Optional<? extends TableCell<C>> cell(C column) {
      int index;
      if (adapter.headerLocator() instanceof RowHeaderCellLocator headers) {
        String expected = resolver.displayedHeader(column);
        if (!row.get().findAll(headers.headerCellLocator()).texts().contains(expected)) {
          return Optional.empty();
        }
        index = 0;
      } else {
        index = SelenideDomTableModel.this.columnIndex(column, resolver);
      }
      final int cellIndex = index;
      ElementsCollection cells = row.get().findAll(adapter.dataCellLocator());
      return index < cells.size()
          ? Optional.of(new SelenideCell(column, () -> {
            ElementsCollection currentCells = row.get().findAll(adapter.dataCellLocator());
            return currentCells.get(cellIndex);
          }))
          : Optional.empty();
    }

    @Override
    public Optional<String> cellText(int index) {
      if (index < 0) {
        throw new IndexOutOfBoundsException(index);
      }
      ElementsCollection cells = row.get().findAll(adapter.dataCellLocator());
      return index < cells.size() ? Optional.of(cells.get(index).text()) : Optional.empty();
    }

    @Override
    public SelenideElement cellElement(int index) {
      return row.get().findAll(adapter.dataCellLocator()).get(index);
    }

    @Override
    public int columnIndex(C column) {
      return adapter.headerLocator() instanceof RowHeaderCellLocator ? 0
          : SelenideDomTableModel.this.columnIndex(column, resolver);
    }

    @Override
    public boolean isVisible() {
      return row.get().isDisplayed();
    }
  }

  private record SelenideCell<C>(C column, Supplier<SelenideElement> element) implements TableCell<C> {
    @Override
    public String text() {
      return element.get().text();
    }
  }

  private static TableDomAdapter adapterFor(DomTableLayout layout) {
    return switch (Objects.requireNonNull(layout, "layout")) {
      case CLASSIC -> TableDomAdapters.classic();
      case FLEX -> TableDomAdapters.flex();
      case HORIZONTAL -> TableDomAdapters.horizontal();
    };
  }
}
