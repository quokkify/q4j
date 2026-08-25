package dev.quokkify.elements.base;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import dev.quokkify.elements.table.classic.base.BaseColumn;
import dev.quokkify.elements.table.classic.base.BaseRow;
import dev.quokkify.elements.table.model.DisplayedHeaderResolver;
import dev.quokkify.elements.table.model.DomTableLayout;
import dev.quokkify.elements.table.model.SelenideDomTableModel;
import dev.quokkify.elements.table.model.SelenideTableQuery;
import dev.quokkify.elements.table.model.TableDomAdapter;
import dev.quokkify.elements.table.model.TableModel;
import dev.quokkify.html.model.HtmlTag;

import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ElementsCollection;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Abstract class to work with table.
 *
 * @param <T> enum with columns enumerations
 */
public abstract class BaseTable<T extends Enum<T>> extends Component {

  protected static final int HEADERS_ROW_INDEX = 0;
  protected static final int HTML_START_INDEX = 1;

  private final Random random = new Random();

  /**
   * Function to get table column index.
   */
  protected abstract Function<T, Integer> fetchColumnIndex();

  /**
   * Resolve a column index from the same table element that a Selenide condition is checking.
   * Dynamic table variants override this method to avoid starting nested Selenide waits.
   */
  protected int fetchColumnIndex(Driver driver, WebElement table, T columnHeader) {
    return fetchColumnIndex().apply(columnHeader);
  }

  /** Exposes this legacy table through the framework-neutral DOM model. */
  public TableModel<T> asDomModel(Function<T, String> displayedHeader) {
    return new SelenideDomTableModel<>(getSelf(), domTableLayout(),
        DisplayedHeaderResolver.requiringNonNull(displayedHeader));
  }

  /** Exposes this table through a caller-supplied DOM adapter. */
  public TableModel<T> asDomModel(TableDomAdapter adapter, Function<T, String> displayedHeader) {
    return new SelenideDomTableModel<>(getSelf(), adapter,
        DisplayedHeaderResolver.requiringNonNull(displayedHeader));
  }

  /** Creates the additive Selenide query layer for this legacy table. */
  public SelenideTableQuery<T> query(Function<T, String> displayedHeader) {
    SelenideDomTableModel<T> model = new SelenideDomTableModel<>(getSelf(), domTableLayout(),
        DisplayedHeaderResolver.requiringNonNull(displayedHeader));
    return new SelenideTableQuery<>(model);
  }

  /** Creates the additive Selenide query layer with a caller-supplied DOM adapter. */
  public SelenideTableQuery<T> query(TableDomAdapter adapter,
                                     Function<T, String> displayedHeader) {
    SelenideDomTableModel<T> model = new SelenideDomTableModel<>(getSelf(), adapter,
        DisplayedHeaderResolver.requiringNonNull(displayedHeader));
    return new SelenideTableQuery<>(model);
  }

  /** DOM shape used by the neutral model bridge. */
  protected DomTableLayout domTableLayout() {
    return DomTableLayout.CLASSIC;
  }

  /**
   * Get first row in table.
   *
   * @return first {@link BaseRow} element
   */
  public abstract BaseRow getFirstRow();

  /**
   * Get all table rows.
   *
   * @return {@link BaseRow} of all table rows
   */
  public abstract List<? extends BaseRow> getAllRows();

  /**
   * Get any random table row.
   *
   * @return any {@link BaseRow} element
   */
  public BaseRow getAnyRow() {
    List<? extends BaseRow> rows = getAllRows();
    return rows.get(random.nextInt(rows.size()));
  }

  /**
   * Checks is table empty.
   *
   * @return true if table has no rows
   */
  public boolean isTableEmpty() {
    return getAllRows().isEmpty();
  }

  /**
   * Get table column.
   *
   * @param columnHeader column enum
   * @return table {@link BaseColumn} element
   */
  public BaseColumn<T> getColumn(T columnHeader) {
    return new BaseColumn<>(getAllColumns().get(fetchColumnIndex().apply(columnHeader)));
  }

  /**
   * Get all table columns names.
   *
   * @return list of table columns names
   */
  public List<String> getAllColumnsNames() {
    return getAllColumns().texts();
  }

  /**
   * Get all table columns.
   *
   * @return {@link ElementsCollection} of all table columns
   */
  protected ElementsCollection getAllColumns() {
    return this.getSelf().findAll(By.tagName(HtmlTag.TH));
  }
}
