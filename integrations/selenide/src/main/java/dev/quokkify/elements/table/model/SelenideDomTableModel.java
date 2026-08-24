package dev.quokkify.elements.table.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

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
    return layout == DomTableLayout.CLASSIC
        ? table.findAll(By.tagName("th")).texts()
        : table.findAll(By.cssSelector("tr th")).texts();
  }

  @Override
  public List<? extends TableRow<C>> rows() {
    ElementsCollection elements = table.findAll(By.tagName("tr"));
    List<TableRow<C>> result = new ArrayList<>();
    int firstDataRow = layout == DomTableLayout.CLASSIC ? 1 : 0;
    for (int index = firstDataRow; index < elements.size(); index++) {
      result.add(new SelenideRow(elements.get(index)));
    }
    return result;
  }

  private final class SelenideRow implements TableRow<C> {
    private final SelenideElement row;

    private SelenideRow(SelenideElement row) {
      this.row = row;
    }

    @Override
    public Optional<? extends TableCell<C>> cell(C column) {
      int index;
      if (layout == DomTableLayout.HORIZONTAL) {
        String expected = resolver.displayedHeader(column);
        if (!row.findAll(By.tagName("th")).texts().contains(expected)) {
          return Optional.empty();
        }
        index = 0;
      } else {
        index = columnIndex(column, resolver);
      }
      ElementsCollection cells = row.findAll(By.tagName("td"));
      return index < cells.size() ? Optional.of(new SelenideCell(column, cells.get(index))) : Optional.empty();
    }
  }

  private record SelenideCell<C>(C column, SelenideElement element) implements TableCell<C> {
    @Override
    public String text() {
      return element.text();
    }
  }
}
