# Table API

A thin, header-aware helper over Selenide in `integrations/selenide`, package `dev.quokkify.elements.table`.
It covers three things plain Selenide does not give: a column addressed by its displayed header, a row lookup
scoped to one column, and a failure on ambiguous headers. Everything else (waiting, assertions, cell actions)
is ordinary Selenide on the returned `SelenideElement` / `ElementsCollection`.

Public types: `Table`, `TableRow`, `TableLayout`, `HorizontalTable`, `TableColumnException`.

## Usage

```java
Table customers = Table.of($("#customers"), TableLayout.html());

customers.row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"));
customers.rows("Country", "Austria").shouldHave(size(2));
customers.column("Company").shouldHave(exactTexts("Alfreds Futterkiste", "Ernst Handel"));
customers.row(0).cell(1).shouldBe(visible);

HorizontalTable contact = HorizontalTable.of($("#horizontal-customers"));
contact.value("Phone").shouldHave(text("+43"));
```

### `Table`

| Method | Meaning |
|---|---|
| `Table.of(root, layout)` | wraps a table-like element |
| `headers()` | `ElementsCollection` of header cells |
| `rows()` | `ElementsCollection` of data rows |
| `row(int)` | row by 0-based index |
| `row(column, value)` | first row whose cell in `column` has exact text `value` |
| `rows(column, value)` | all such rows |
| `column(header)` | cells of that column across rows |
| `root()` | the root `SelenideElement` |

### `TableRow`

`cell(String header)`, `cell(int)`, `cells()`, `self()`.

### `HorizontalTable`

Key/value tables where each `<tr>` holds a `<th>` label and a `<td>` value.
`value(header)` returns the `td` of the row whose `th` has exact text `header`; it is lazy and fails with
Selenide `ElementNotFound` for a missing label. Duplicate labels resolve to the first row; there is no
duplicate-label detection. `headers()` returns all `th`.

## Layouts

`TableLayout` is an immutable record of three relative locators: `rows` and `headers` relative to the table
root, `cells` relative to a row.

| Factory | rows | cells | headers |
|---|---|---|---|
| `html()` | `./tbody/tr[td]` | `./*[self::td or self::th]` | `./thead/tr/th` |
| `aria()` | `.//*[@role='row'][*[@role='cell' or @role='gridcell']]` | `./*[@role='cell' or @role='gridcell' or @role='rowheader']` | `.//*[@role='columnheader']` |
| `of(By rows, By cells, By headers)` | caller-defined | caller-defined | caller-defined |

- `html()` uses direct-child XPath, so rows of nested tables are not counted. It needs a `<thead>`; a table
  whose header row sits inside `<tbody>` uses `of(...)`:

  ```java
  TableLayout.of(By.xpath("./tbody/tr[td]"), By.xpath("./*[self::td or self::th]"), By.xpath("./tbody/tr[th]/th"))
  ```

- `aria()` matches explicit `role` attributes only. Being descendant-based, it also matches nested ARIA grids;
  use `of(...)` for those.
- Div grids are described with `of(...)`:

  ```java
  TableLayout.of(
      By.cssSelector(":scope > .flex-table-row:not(:first-child)"),
      By.cssSelector(":scope > div"),
      By.cssSelector(":scope > .flex-table-row:first-child > div"))
  ```

- `rowspan` / `colspan` are not modelled: only physical DOM cells are addressed.

## Behaviour and limits

### Matching

- Headers are matched exactly and case-sensitively after trimming the displayed text.
- `row(column, value)` and `rows(column, value)` compare the trimmed visible text (`getText()`) of the cell in
  that column only; a value in a neighbouring column is ignored. Hidden rows and cells read as `""` there,
  while `column(...).texts()` / `exactTexts(...)` still report hidden cell text (Selenide semantics).
- A row with fewer cells than the column index does not match.
- Hidden rows count in indexes like in any `ElementsCollection`.
- `row(column, value)` returns the first match in DOM order. Check uniqueness explicitly:
  `table.rows(column, value).shouldHave(size(1))`.
- A missing row surfaces when the returned element is used, as Selenide `ElementNotFound` whose message
  contains the condition, for example `Company = "Ernst Handel"`.

### Laziness and remount safety

`Table`, `TableRow` and everything they return hold locators, not `WebElement`s, so references captured before
the table root is replaced keep working. `row(column, value)` re-resolves the column index on every evaluation,
so it survives header reordering and waits for delayed rows through the normal `should*` timeout.

`cell(header)` resolves the column index when it is called (waiting for the headers with Selenide's default
timeout) and returns an element that is lazy by index. Call `cell(header)` again after columns are reordered.

### Header errors

`TableColumnException` is unchecked. Its message names the header, the reason (`not found` or `ambiguous`),
the table root and the list of displayed headers.

- In `cell(header)` and `column(header)` a missing or ambiguous header throws immediately.
  Both first wait for at least one header with Selenide's default timeout; if headers never mount, the failure
  is a Selenide collection-size assertion, not `TableColumnException`.
- Inside a `row(column, value)` lookup it surfaces, unwrapped, only after the `should*` timeout, so a typo in a
  column name costs one full timeout.
- While no headers are mounted yet (empty header list), a row lookup keeps waiting instead of failing.

### `column(header)`

Works only for XPath layouts whose cells locator is a single child step starting with `./` (`html()`,
`aria()`). A layout built with CSS `of(...)` throws `UnsupportedOperationException`; use `rows()` with
`TableRow.cell(...)` instead. The column index is resolved once, when `column` is called.

## Not covered

Table-specific assertions or conditions, typed cell values, wrappers for controls inside cells, sorting,
filtering, pagination, virtual scrolling, page-factory (`@FindBy`) injection.

## Migration from the removed table stack

This is a breaking change; there is no deprecation period and no bridge to the old types.

| Removed | Replacement |
|---|---|
| `table/classic/*`: `Table`, `DynamicTable`, `FlexTable`, `SelenideDataTable`, `Row`, `Cell`, bases | `Table` + `TableLayout.html()` / `of(...)` |
| `table/horizontal/*` | `HorizontalTable` |
| `table/model/*`: `TableModel<C>`, `TableDomAdapter`, `SelenideTableQuery`, row/column/table assertions, controls, typed refs, `RowData`, `RowConditions`, `ExpectedValue`, exceptions | `TableLayout`, `Table`, `TableRow`, `TableColumnException`, native Selenide conditions and actions |
| `elements/base/BaseTable` | none |
| `ex/TableRowException` | Selenide `ElementNotFound` |
