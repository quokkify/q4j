package dev.quokkify.elements.table.classic;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import dev.quokkify.elements.base.Component;
import dev.quokkify.elements.table.model.RowAssertion;
import dev.quokkify.elements.table.model.RowAssertions;
import dev.quokkify.elements.table.model.RowCondition;
import dev.quokkify.elements.table.model.RowConditions;
import dev.quokkify.elements.table.model.SelenideTableQuery;
import dev.quokkify.elements.table.model.TableAssertions;
import dev.quokkify.elements.table.model.TableDomAdapter;
import dev.quokkify.elements.table.model.TableDomAdapters;

import com.codeborne.selenide.WebElementCondition;

/**
 * String-first, {@code @FindBy}-friendly data table component.
 *
 * <p>The component keeps only lazy Selenide references. The table root, selected rows, and cells
 * are resolved again by Selenide for each operation, so delayed rendering and root remounts are
 * handled by the normal Selenide wait loop.</p>
 */
public class SelenideDataTable extends Component {

  private final TableDomAdapter adapter;

  /** Creates a semantic HTML table component using the classic adapter. */
  public SelenideDataTable() {
    this(TableDomAdapters.classic());
  }

  /** Creates a component with an explicit DOM adapter for custom table markup. */
  public SelenideDataTable(TableDomAdapter adapter) {
    this.adapter = Objects.requireNonNull(adapter, "adapter");
  }

  /** Returns the lazy query using exact displayed header text as the column identity. */
  public SelenideTableQuery<String> query() {
    return SelenideTableQuery.byHeaderText(getSelf(), adapter);
  }

  /** Requires the first row matching the supplied condition. */
  public dev.quokkify.elements.table.model.TableQueryRow<String> requiredRow(RowCondition<String> condition) {
    return query().requiredRow(Objects.requireNonNull(condition, "condition"));
  }

  /** Requires the first row matching the supplied exact cell values. */
  public dev.quokkify.elements.table.model.TableQueryRow<String> requiredRow(Map<String, String> values) {
    return requiredRow(exactValues(values));
  }

  /** Asserts that at least one row has all supplied exact cell values. */
  public SelenideDataTable shouldHaveRow(Map<String, String> values) {
    query().shouldHave(TableAssertions.matchingRow(exactValueAssertions(values)));
    return this;
  }

  /** Asserts that the table contains a row matching the supplied condition. */
  public SelenideDataTable shouldHaveRow(RowCondition<String> condition) {
    query().requiredRow(Objects.requireNonNull(condition, "condition"));
    return this;
  }

  /** Asserts that the table contains a row matching all supplied exact values. */
  public SelenideDataTable shouldHave(Map<String, String> values) {
    return shouldHaveRow(values);
  }

  /** Returns a row assertion combining conditions for several displayed headers. */
  public static RowAssertion<String> rowConditions(Map<String, WebElementCondition> conditions) {
    Objects.requireNonNull(conditions, "conditions");
    RowAssertion<String> result = null;
    for (Map.Entry<String, WebElementCondition> entry : sorted(conditions).entrySet()) {
      RowAssertion<String> current = RowAssertions.cell(entry.getKey(),
          Objects.requireNonNull(entry.getValue(), "condition for '" + entry.getKey() + "'"));
      result = result == null ? current : result.and(current);
    }
    return result == null ? RowAssertions.values() : result;
  }

  private static RowCondition<String> exactValues(Map<String, String> values) {
    Objects.requireNonNull(values, "values");
    RowCondition<String> result = null;
    for (Map.Entry<String, String> entry : sorted(values).entrySet()) {
      RowCondition<String> current = dev.quokkify.elements.table.model.RowConditions.exact(entry.getKey(),
          Objects.requireNonNull(entry.getValue(), "value for '" + entry.getKey() + "'"));
      result = result == null ? current : RowConditions.all(result, current);
    }
    return result == null ? RowConditions.all() : result;
  }

  private static RowAssertion<String> exactValueAssertions(Map<String, String> values) {
    Objects.requireNonNull(values, "values");
    RowAssertion<String> result = null;
    for (Map.Entry<String, String> entry : sorted(values).entrySet()) {
      RowAssertion<String> current = RowAssertions.cell(entry.getKey(),
          com.codeborne.selenide.Condition.exactTextCaseSensitive(
              Objects.requireNonNull(entry.getValue(), "value for '" + entry.getKey() + "'")));
      result = result == null ? current : result.and(current);
    }
    return result == null ? RowAssertions.values() : result;
  }

  private static <V> Map<String, V> sorted(Map<String, V> values) {
    TreeMap<String, V> sorted = new TreeMap<>();
    values.forEach((key, value) -> sorted.put(Objects.requireNonNull(key, "header"), value));
    return sorted;
  }
}
