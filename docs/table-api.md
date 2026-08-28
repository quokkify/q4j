# Table API and extension boundary

Status: accepted documentation for the `main` branch at `3d7020342`.

This document describes the table API shipped by q4j. It is deliberately an API record, not a history of the table pull requests.

## Public type and dependency map

The public structural contract is in `dev.quokkify.elements.table.model`:

- `TableModel<C>` exposes displayed headers, currently mounted rows, typed-column lookup, and optional/required row lookup.
- `TableRow<C>` exposes an optional cell and a required-cell convenience method.
- `TableCell<C>` exposes its typed column key and current text.
- `DisplayedHeaderResolver<C>` maps a caller-owned key to the header text displayed by the DOM.

`SelenideDomTableModel<C>` is the Selenide implementation of `TableModel<C>`. It depends on
`SelenideElement`, `ElementsCollection`, and Selenium `By`; those are intentionally not part of
the neutral structural contract. `TableDomAdapter` is a Selenide/Selenium-specific description of
relative root, row, cell, and header locators. `TableDomAdapters` supplies `classic()`, `flex()`,
`horizontal()`, `ariaGrid()`, and `of(...)`.

The additive Selenide navigation layer consists of `SelenideTableQuery`, row/column/cell
references, `RowConditions`, and the table/row/column assertion and action handles. Legacy
`Table`, `DynamicTable`, `FlexTable`, `HorizontalTable`, and their existing FQCNs remain
available. `DomTableLayout` and its compatibility constructor bridge the old layout enum.

## Consumer examples

A built-in HTML table can be exposed from a page object without relying on enum ordinal order:

```java
TableModel<Column> model = SelenideDomTableModel.of(
    Selenide.$("#customers"),
    TableDomAdapters.classic(),
    DisplayedHeaderResolver.requiringNonNull(Column::displayed));

TableRow<Column> row = model.requiredRow(
    candidate -> candidate.cell(Column.COMPANY)
        .map(cell -> cell.text().equals("Alfreds")).orElse(false),
    "company", Duration.ofSeconds(2));

assertThat(row.requiredCell(Column.COUNTRY).text()).isEqualTo("Austria");
```

A project-specific DOM shape uses relative locators. The selectors are evaluated below the current
root, so nested tables are not accidentally included when the row selector is scoped:

```java
TableDomAdapter grid = TableDomAdapters.of(
    By.cssSelector(":scope > .data-row"),
    By.cssSelector(":scope > .cell:not([hidden])"),
    new TableHeaderRowLocator(
        By.cssSelector(":scope > .header-row"),
        By.cssSelector(":scope > .cell:not([hidden])")));
TableModel<Column> customDom = SelenideDomTableModel.of(
    Selenide.$("#project-grid"), grid,
    DisplayedHeaderResolver.requiringNonNull(Column::displayed));
```

A fully custom backend does not need Selenide or Selenium. Implement the three structural
interfaces and keep any lookup, remount, or transport policy in that implementation:

```java
import java.util.List;
import java.util.Map;
import java.util.Optional;
import dev.quokkify.elements.table.model.TableCell;
import dev.quokkify.elements.table.model.TableModel;
import dev.quokkify.elements.table.model.TableRow;

enum Column { COUNTRY, COMPANY }

final class ApiModel implements TableModel<Column> {
  private final List<ApiRow> dataRows = List.of(new ApiRow(Map.of(
      Column.COUNTRY, "Austria", Column.COMPANY, "Alfreds")));

  @Override public List<String> displayedHeaders() { return List.of("Country", "Company"); }
  @Override public List<ApiRow> rows() { return dataRows; }
}

final class ApiRow implements TableRow<Column> {
  private final Map<Column, String> values;
  ApiRow(Map<Column, String> values) { this.values = Map.copyOf(values); }
  @Override public Optional<ApiCell> cell(Column key) {
    return Optional.ofNullable(values.get(key)).map(value -> new ApiCell(key, value));
  }
}

record ApiCell(Column column, String text) implements TableCell<Column> {}
```

This backend-neutral example proves only the structural model contract. Header exclusion, nested
DOM isolation, remount behavior, and timeout policy belong to the concrete backend and are not
claims of this example. The compiling equivalent is exercised by
`TableModelContractTest.supportsFullyCustomBackendContract`.

## Semantics and compatibility

- Typed keys are resolved by displayed header text; enum ordinal is never a column position.
- `displayedHeaders()` and `rows()` reflect the model's current view. A row or cell may be lazy.
- Selenide-backed rows, columns, and cells retain locators/indexes, not raw `WebElement` objects;
  each operation re-resolves the current root. Handles therefore remain usable after a root
  replacement, provided the replacement still satisfies the adapter contract.
- `rows()` and `row(predicate)` are non-waiting status reads. Selenide's `requiredRow(..., timeout)`
  uses one Selenide condition loop and one caller-provided timeout across root and row discovery.
- A missing row is `TableRowNotFoundException`; a missing displayed header is
  `TableColumnNotFoundException`; a repeated displayed header is `TableColumnAmbiguousException`;
  a missing required cell is `TableCellNotFoundException`. `uniqueRow` throws
  `TableRowAmbiguousException` when more than one row matches and includes the observed match
  count. Required-row diagnostics include the caller description and timeout; column-not-found
  diagnostics include the requested key, its displayed name, and available headers. Optional row
  and cell lookups return `Optional.empty()` for absence, while required variants throw the typed
  not-found exception.
- A mounted empty cell is present and has empty text. A missing cell is absent.
- `findRow`/`requiredRow` select the first match; `findRows` preserves all matches in DOM order;
  `uniqueRow` rejects both zero and multiple matches.
- The built-in classic adapter accepts ordinary `<td>` data cells and semantic
  `<th scope="row">` cells. Header-only rows are excluded. Relative selectors also exclude rows
  owned by nested tables; a nested table can be selected separately as its own model root.
- Flex, horizontal, and ARIA-grid adapters use their documented row/header semantics. Hidden cells
  can be excluded by a custom adapter's locator. Non-rectangular rows remain deterministic: a
  missing cell is absent rather than synthesized.

Released `0.6.0` FQCNs and signatures are preserved. New consumers should prefer the structural
interfaces and `TableDomAdapters.of(...)`; no existing legacy type is renamed or removed.

## Compatibility boundary and Appium seam

The structural interfaces (`TableModel<C>`, `TableRow<C>`, and `TableCell<C>`) are the
backend-neutral extension boundary. A future Appium implementation could be shaped as:

```java
abstract class AppiumTableModel<C> implements TableModel<C> {
  // Appium locator/driver policy stays in this implementation.
}
```

This is an architecture sketch only and is intentionally abstract. q4j currently provides no Appium dependency, type, driver
setup, fixture, or runtime implementation. `TableDomAdapter`, `SelenideDomTableModel`,
`SelenideTableQuery`, and all assertion/action handles are browser/Selenium/Selenide-specific;
Selenium `By` and Selenide `SelenideElement` must not be described as framework-neutral. An Appium
adapter must not change these existing FQCNs.

## Weakness and evidence matrix

| Weakness or ambiguity                                                 | Affected API                            | Consumer impact                                      | Compatibility risk                      | Evidence                                                                                                                                             | Action                                                             |
| --------------------------------------------------------------------- | --------------------------------------- | ---------------------------------------------------- | --------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| Header lookup by display text can be ambiguous                        | `TableModel.columnIndex`                | A duplicate heading cannot be addressed safely       | Low; exception is additive behavior     | Contract test covers repeated `Company`                                                                                                              | Keep; clarify                                                      |
| Headerless typed lookup has no implicit position                      | `TableHeaderLocator`, `TableModel`      | Consumers must provide a different key strategy      | Low                                     | Headerless adapter test expects typed lookup failure                                                                                                 | Clarify                                                            |
| Sealed header strategy limits third-party header strategies           | `TableHeaderLocator`                    | Custom DOM uses the existing three shapes only       | High if unsealed/changed after 0.6.0    | Source audit; `of(...)` supports custom row/cell locators without a new strategy                                                                     | Defer to future major; use `TableModel` for a fully custom backend |
| Adapter output can be non-rectangular                                 | `TableDomAdapter`, `TableRow.cell`      | Missing cells must be distinguished from empty cells | Low                                     | `TableModelContractTest.supportsCustomDivAdapter` asserts a mounted empty cell is present with empty text and a short-row cell is `Optional.empty()` | Keep; clarify                                                      |
| Nested rows can leak if selectors are broad                           | `TableDomAdapter.mountedDataRowLocator` | Outer queries may count inner rows                   | Low                                     | `TableModelContractTest.supportsCustomDivAdapter` and `customGridAdapter()` use direct-child selectors with nested fixture `#nested-custom-grid`     | Keep; clarify selector scope                                       |
| DOM root/remount can stale cached elements                            | Selenide model and handles              | Previously returned handles could fail after refresh | Medium                                  | `TableModelContractTest.customAdapterWaitsAndSurvivesRemount` and `remountCustomGrid()`                                                              | Keep; clarify guarantee                                            |
| Timeout could be spent once per nested wait                           | `requiredRow(..., Duration)`            | Slow or surprising failure timing                    | Medium                                  | `TableModelContractTest.customAdapterWaitsAndSurvivesRemount` and `prepareCustomDelayed()`                                                           | Keep; clarify                                                      |
| Legacy and neutral APIs duplicate some table traversal                | Legacy table classes vs neutral model   | Package discovery is harder                          | Medium; removal breaks released callers | README and bridge tests enumerate both surfaces                                                                                                      | Clarify; no removal                                                |
| Selenide-specific handles could be mistaken for neutral API           | Query/assertion/action types            | Non-browser consumers may choose the wrong boundary  | Low                                     | Type/dependency map and compile-only custom model example                                                                                            | Clarify                                                            |
| Sorting/filtering/pagination/virtualization/pinned columns are absent | Structural model                        | Consumers need separate capabilities                 | High to add semantics prematurely       | No production API or tests claim these behaviors                                                                                                     | Reject for this issue; defer                                       |
| Appium implementation is absent                                       | Selenide adapter layer                  | Appium users need an implementation outside q4j      | Low                                     | No Appium dependency or runtime code by design                                                                                                       | Future; reserved seam                                              |

## Non-goals and deferred shapes

This issue does not add sorting, filtering, pagination, selection, editing, virtualization, infinite
scrolling, pinned/frozen columns, merged cells, or expandable/tree/master-detail rows. Nested tables
are supported only as isolated model roots; there is no nested-table expansion API. These shapes need
separate contracts before a compatible API can be designed.

## Verification map

`TableModelContractTest` is hermetic and exercises ordinary `<td>` rows, semantic row headers,
header-only exclusion, a direct-child custom div-grid adapter, hidden cells, a mounted empty cell
and an omitted short-row cell,
non-rectangular row, nested custom-grid isolation, delayed custom rendering, custom root remount,
duplicate/headerless failures, and a fully custom backend. `TableQueryContractTest` and `TableAssertionsActionsContractTest` cover the
Selenide-specific navigation and handles. No external site or Appium runtime is required.
