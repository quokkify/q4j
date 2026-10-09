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

| Method                   | Meaning                                                               |
| ------------------------ | --------------------------------------------------------------------- |
| `Table.of(root, layout)` | wraps a table-like element                                            |
| `headers()`              | `ElementsCollection` of header cells                                  |
| `rows()`                 | `ElementsCollection` of data rows                                     |
| `row(int)`               | row by 0-based index                                                  |
| `row(column, value)`     | first row whose cell in `column` has exact text `value`               |
| `row(SelenideElement)`   | wraps a row element you found yourself, so `cell(header)` works on it |
| `rows(column, value)`    | all such rows                                                         |
| `column(header)`         | cells of that column across rows                                      |
| `root()`                 | the root `SelenideElement`                                            |

### `TableRow`

`cell(String header)`, `cell(int)`, `cells()`, `self()`.

`Table.row(SelenideElement)` turns any (lazy) row element into a `TableRow`. The element must be a row of the
same table that matches the layout's cells locator:

```java
customers.row(customers.column("Company").findBy(matchText("Ernst.*")).closest("tr"))
    .cell("Country").shouldHave(exactText("Austria"));
```

### `HorizontalTable`

Key/value tables where each `<tr>` holds a `<th>` label and a `<td>` value.
`value(header)` returns the `td` of the row whose `th` has exact text `header`; it is lazy and fails with
Selenide `ElementNotFound` for a missing label. Duplicate labels resolve to the first row; there is no
duplicate-label detection. `headers()` returns all `th`.

## Layouts

`TableLayout` is an immutable record of three relative locators: `rows` and `headers` relative to the table
root, `cells` relative to a row.

| Factory                             | rows                                                     | cells                                                        | headers                      |
| ----------------------------------- | -------------------------------------------------------- | ------------------------------------------------------------ | ---------------------------- |
| `html()`                            | `./tbody/tr[td]`                                         | `./*[self::td or self::th]`                                  | `./thead/tr/th`              |
| `aria()`                            | `.//*[@role='row'][*[@role='cell' or @role='gridcell']]` | `./*[@role='cell' or @role='gridcell' or @role='rowheader']` | `.//*[@role='columnheader']` |
| `of(By rows, By cells, By headers)` | caller-defined                                           | caller-defined                                               | caller-defined               |

- `html()` uses direct-child XPath, so rows of nested tables are not counted. It needs a `<thead>`; a table
  whose header row sits inside `<tbody>` uses `of(...)`:

  ```java
  TableLayout.of(By.xpath("./tbody/tr[td]"), By.xpath("./*[self::td or self::th]"), By.xpath("./tbody/tr[1]/th"))
  ```

- `html()` needs `<tbody>` in the DOM: tables built through DOM APIs without a `<tbody>` yield no rows.
  `<tfoot>` rows are not included.
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

- Headers are matched exactly and case-sensitively after whitespace normalization: every run of whitespace,
  including the no-break space U+00A0, becomes one space and the result is trimmed. The requested header and
  value are normalized the same way, so `"Company  Name"` on screen matches `"Company Name"`.
- `row(column, value)` and `rows(column, value)` compare the normalized visible text (`getText()`) of the cell in
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

`cell(header)` resolves the column index when it is called (waiting for the requested header with Selenide's default
timeout) and returns an element that is lazy by index. Call `cell(header)` again after columns are reordered.

### Header errors

`TableColumnException` is unchecked. Its message names the header, the reason (`not found` or `ambiguous`),
the table root and the list of displayed headers.

- `cell(header)` and `column(header)` wait, with Selenide's default timeout, until the requested header is
  displayed. If it never appears they throw `TableColumnException` (`not found`) with the headers displayed at
  that moment (`[]` if none mounted). An ambiguous header throws as soon as the requested header is displayed.
- Inside a `row(column, value)` or `rows(column, value)` lookup, a column that is not displayed (or no headers
  mounted yet) does not throw: the lookup keeps waiting, so a late-rendered column is picked up within the
  `should*` timeout. A typo in the column name therefore fails only after that timeout, with Selenide's
  `ElementNotFound` for `row(...)` or the collection assertion error for `rows(...)`, not with
  `TableColumnException`; the message contains the condition (for example `Region = "x"`).
- An ambiguous column inside a `row(column, value)` or `rows(column, value)` lookup throws `TableColumnException`
  (`ambiguous`) immediately, without waiting for the timeout.

### `column(header)`

Works only for XPath layouts whose cells locator is a single child step starting with `./` (`html()`,
`aria()`). A layout built with CSS `of(...)` throws `UnsupportedOperationException`; use `rows()` with
`TableRow.cell(...)` instead. A cells locator starting with `.//` is rejected the same way. The column index is
resolved once, when `column` is called.

### Cost

Each row evaluation in `row(column, value)` / `rows(column, value)` makes several WebDriver calls (headers, the
row's cells, the cell text); on very large tables prefer a narrow layout (for example rows limited by an XPath
predicate) so fewer rows are evaluated.

## Not covered

Table-specific assertions or conditions, typed cell values, wrappers for controls inside cells, sorting,
filtering, pagination, virtual scrolling, page-factory (`@FindBy`) injection.

## Migration from the removed table stack

This is a breaking change; there is no deprecation period and no bridge to the old types.

| Removed                                                                                                                                                                               | Replacement                                                                                        |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| `table/classic/*`: `Table`, `DynamicTable`, `FlexTable`, `SelenideDataTable`, `Row`, `Cell`, bases                                                                                    | `Table` + `TableLayout.html()` / `of(...)`                                                         |
| `table/horizontal/*`                                                                                                                                                                  | `HorizontalTable`                                                                                  |
| `table/model/*`: `TableModel<C>`, `TableDomAdapter`, `SelenideTableQuery`, row/column/table assertions, controls, typed refs, `RowData`, `RowConditions`, `ExpectedValue`, exceptions | `TableLayout`, `Table`, `TableRow`, `TableColumnException`, native Selenide conditions and actions |
| `elements/base/BaseTable`                                                                                                                                                             | none                                                                                               |
| `ex/TableRowException`                                                                                                                                                                | Selenide `ElementNotFound`                                                                         |
