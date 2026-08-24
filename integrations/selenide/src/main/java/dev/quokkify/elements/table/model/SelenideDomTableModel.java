package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.impl.WebElementWrapper;
import org.openqa.selenium.By;
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
  private final DomTableLayout layout;
  private final DisplayedHeaderResolver<C> resolver;

  public SelenideDomTableModel(SelenideElement table, DomTableLayout layout,
                               DisplayedHeaderResolver<C> resolver) {
    this.table = Objects.requireNonNull(table, "table");
    this.layout = Objects.requireNonNull(layout, "layout");
    this.resolver = Objects.requireNonNull(resolver, "resolver");
  }

  @Override
  public List<String> displayedHeaders() {
    return layout == DomTableLayout.FLEX
        ? table.findAll(By.cssSelector(".flex-table-row:first-child > div")).texts()
        : table.findAll(By.tagName("th")).texts();
  }

  @Override
  public List<? extends TableRow<C>> rows() {
    if (!table.exists()) {
      return List.of();
    }
    int firstDataRow = layout == DomTableLayout.HORIZONTAL ? 0 : 1;
    return new AbstractList<>() {
      @Override
      public TableRow<C> get(int index) {
        if (index < 0 || index >= size()) {
          throw new IndexOutOfBoundsException(index);
        }
        return new SelenideRow(() -> rowsElements().get(firstDataRow + index));
      }

      @Override
      public int size() {
        return Math.max(0, rowsElements().size() - firstDataRow);
      }
    };
  }

  @Override
  public TableRow<C> requiredRow(Predicate<TableRow<C>> predicate, String description, Duration timeout) {
    try {
      if (!table.exists()) {
        table.shouldBe(Condition.exist, timeout);
      }
      SelenideElement matching = rowsElements().findBy(new MatchingRowCondition(predicate, description));
      matching.shouldBe(Condition.exist, timeout);
      return new SelenideRow(() -> matching);
    } catch (UIAssertionError error) {
      TableRowNotFoundException failure = new TableRowNotFoundException(
          description + "; timeout=" + timeout);
      failure.initCause(error);
      throw failure;
    }
  }

  private ElementsCollection rowsElements() {
    return layout == DomTableLayout.FLEX
        ? table.findAll(By.cssSelector(".flex-table-row"))
        : table.findAll(By.tagName("tr"));
  }

  private final class MatchingRowCondition extends WebElementCondition {
    private final Predicate<TableRow<C>> predicate;

    private MatchingRowCondition(Predicate<TableRow<C>> predicate, String description) {
      super(description);
      this.predicate = predicate;
    }

    @Override
    public CheckResult check(Driver driver, WebElement element) {
      try {
        TableRow<C> candidate = new SelenideRow(
            () -> WebElementWrapper.wrap(driver, element, "table row candidate"));
        return predicate.test(candidate)
            ? CheckResult.accepted("matched row")
            : CheckResult.rejected("row does not match", element.getText());
      } catch (NoSuchElementException | StaleElementReferenceException error) {
        return CheckResult.rejected(error.toString(), "table changed while checking rows");
      }
    }
  }

  private final class SelenideRow implements TableRow<C> {
    private final Supplier<SelenideElement> row;

    private SelenideRow(Supplier<SelenideElement> row) {
      this.row = row;
    }

    @Override
    public Optional<? extends TableCell<C>> cell(C column) {
      int index;
      if (layout == DomTableLayout.HORIZONTAL) {
        String expected = resolver.displayedHeader(column);
        if (!row.get().findAll(By.tagName("th")).texts().contains(expected)) {
          return Optional.empty();
        }
        index = 0;
      } else {
        index = columnIndex(column, resolver);
      }
      final int cellIndex = index;
      ElementsCollection cells = layout == DomTableLayout.FLEX
          ? row.get().findAll(By.cssSelector(":scope > div"))
          : row.get().findAll(By.tagName("td"));
      return index < cells.size()
          ? Optional.of(new SelenideCell(column, () -> {
            ElementsCollection currentCells = layout == DomTableLayout.FLEX
                ? row.get().findAll(By.cssSelector(":scope > div"))
                : row.get().findAll(By.tagName("td"));
            return currentCells.get(cellIndex);
          }))
          : Optional.empty();
    }
  }

  private record SelenideCell<C>(C column, Supplier<SelenideElement> element) implements TableCell<C> {
    @Override
    public String text() {
      return element.get().text();
    }
  }
}
