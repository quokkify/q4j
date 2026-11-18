# Table API

A thin, header-aware helper over Selenide in `integrations/selenide`, package `dev.quokkify.elements.table`.
It covers three things plain Selenide does not give: a column addressed by its displayed header, a row lookup
scoped to one column, and a failure on ambiguous headers. Everything else (waiting, assertions, cell actions)
is ordinary Selenide on the returned `SelenideElement` / `ElementsCollection`.

Public types: `Table`, `TableRow`, `TableLayout`, `HorizontalTable`, `TableColumnException`, and for bulk reads
`TableSnapshot`, `SnapshotRow`, `SnapshotCell`.

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
| `snapshot()`             | headers and every row's cell texts, read in one browser call          |
| `snapshot(By items)`     | same, plus the texts of `items` inside each cell                      |

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
duplicate-label detection. `headers()` returns the `th` of the table's own rows. Only the table's own rows
(`./tr`, `./tbody/tr`, `./thead/tr`, `./tfoot/tr`) are read, so labels of a table nested in a value cell are
ignored.

## Recipe: tables in page objects (`@FindBy`)

A `@FindBy Table` field stays `null`: wrap the helper in a project `Component` and reuse it on any page.

```java
public class OrdersTable extends Component {

  public Table table() {
    return Table.of(getSelf(), TableLayout.html());
  }

  public TableRow order(String id) {
    return table().row("ID", id);
  }
}

@FindBy(css = "table.orders")
private OrdersTable orders;

orders.order("A-1").cell("Status").shouldHave(exactText("New"));
```

Example: `OrdersTablePageObjectTest`.

## Layouts

`TableLayout` is an immutable record of three relative locators: `rows` and `headers` relative to the table
root, `cells` relative to a row.

| Factory                             | rows                                                     | cells                                                        | headers                                                                                                    |
| ----------------------------------- | -------------------------------------------------------- | ------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------- |
| `html()`                            | `./tbody/tr[td]`                                         | `./*[self::td or self::th]`                                  | `./thead/tr[th][last()]/*[self::th or self::td] \| ./thead[not(tr/th)]/tr[last()]/*[self::th or self::td]` |
| `aria()`                            | `.//*[@role='row'][*[@role='cell' or @role='gridcell']]` | `./*[@role='cell' or @role='gridcell' or @role='rowheader']` | `.//*[@role='columnheader']`                                                                               |
| `of(By rows, By cells, By headers)` | caller-defined                                           | caller-defined                                               | caller-defined                                                                                             |

- `html()` reads headers, as `th` or `td`, from the last `<thead>` row that contains a `th`, so a filter row of
  inputs under the header row is skipped; when no row has a `th`, the last `<thead>` row is used. Grouped
  headers (a `colspan` row above the leaf row) work only when that row lists every leaf column; `rowspan` leaf
  headers are not supported.
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

`snapshot()` is the exception: it returns plain data read once (see [Bulk read](#bulk-read-snapshot)).

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
predicate) so fewer rows are evaluated. To read a whole table, use `snapshot()` (see [Bulk read](#bulk-read-snapshot)).

## Bulk read (`snapshot`)

Reading every row through `rows()` and `TableRow.cell(header)` costs about ten WebDriver round trips per row,
which takes tens of seconds on a table with hundreds of rows. `snapshot()` reads the headers and every row's cell
texts in one browser call and returns immutable data to assert on with AssertJ:

```java
Table grid = Table.of($("#properties"), TableLayout.aria());
grid.rows().shouldHave(sizeGreaterThan(0));                  // wait first: a snapshot does not wait for rows

TableSnapshot snap = grid.snapshot(By.cssSelector(".chip"));  // or snapshot() without items
assertThat(snap.column("Name")).contains("Alpha");
assertThat(snap.columnValues("Properties").get(0)).containsExactly("Red", "Blue");
assertThat(snap.rows().get(1).cell("Name").text()).isEqualTo("Beta");
```

| Type            | Content                                                                                     |
| --------------- | ------------------------------------------------------------------------------------------- |
| `TableSnapshot` | `headers()`, `rows()`, `column(header)` (texts), `columnValues(header)` (items per row)     |
| `SnapshotRow`   | `cells()`, `cell(int)`, `cell(header)`                                                      |
| `SnapshotCell`  | `text()`, `values()` (texts of the `items` found inside the cell; empty without `items`)   |

- **Point in time.** The snapshot waits only for the root to exist (Selenide's default timeout). It does not wait
  for rows, does not retry and does not follow remounts: wait for the expected state first.
- **Multi-value cells.** Pass a locator for the items inside a cell (chips, tags) to `snapshot(By items)`; it is
  evaluated relative to every cell. Do not split `text()` on line breaks: whether items are separated by `\n`
  or by spaces depends on the page's CSS.
- **Locators.** The layout and `items` must be `By.xpath` or `By.cssSelector`; any other `By` throws
  `UnsupportedOperationException` before the browser is called.
- **Text.** Headers, cell texts and item texts are whitespace-normalized like header lookup. A hidden cell reads
  as `""`, as in the lazy API; hidden items are skipped. Hidden means no layout box (`display: none`, `hidden`),
  `visibility: hidden` or `opacity: 0` on the element or an ancestor; a `display: contents` element counts as
  visible when any of its children is. Text clipped by an `overflow: hidden` ancestor is still read.
- **Headers.** `column`, `columnValues` and `cell(header)` follow the [header rules](#header-errors) without
  waiting: `TableColumnException` with `not found` or `ambiguous`.
- **Short rows.** `column(...)` gives `""` (and `columnValues(...)` an empty list) for a row with too few cells, so
  the result stays aligned with `rows()`; `cell(...)` on such a row throws `NoSuchElementException`.
- **Mounted rows only.** Virtualized grids return the rows currently in the DOM.

Selenide already reads a whole collection's texts in one browser call, so a single column of an XPath layout is
fast without a snapshot, and its assertion waits and retries:
`table.column("Name").shouldHave(exactTexts(...))`. Use `snapshot()` when you need several columns aligned by row,
items grouped per cell, or a CSS layout (where `column(...)` is unsupported). Note that `column(...).texts()` also
reports hidden cell text, while a snapshot reads hidden cells as `""`. Prefer the lazy API for small tables and for
assertions that should wait and retry.

## Known limitations

- Header text must match exactly: icons or badges inside a `th` are part of its text (`Company ▲`). Use a custom
  headers locator that selects the label, for example `./thead/tr/th/span[@class='label']`.
- Values are compared by visible text; an `<input>` or `<select>` reads as `""`. Find such a row yourself and
  wrap it with `row(SelenideElement)`.
- `colspan` / `rowspan` in the body are not supported.
- Grouped headers require the header row to list every leaf column. A leaf header that spans both rows with
  `rowspan` is missing from that row and shifts the columns silently; describe such a table with `of(...)` and an
  explicit headers locator.

## Not covered

Table-specific assertions or conditions, typed cell values, wrappers for controls inside cells, sorting,
filtering, pagination, virtual scrolling, page-factory (`@FindBy`) injection (see the page-object recipe above).

## Migration from the removed table stack

This is a breaking change; there is no deprecation period and no bridge to the old types.

| Removed                                                                                                                                                                               | Replacement                                                                                        |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| `table/classic/*`: `Table`, `DynamicTable`, `FlexTable`, `SelenideDataTable`, `Row`, `Cell`, bases                                                                                    | `Table` + `TableLayout.html()` / `of(...)`                                                         |
| `table/horizontal/*`                                                                                                                                                                  | `HorizontalTable`                                                                                  |
| `table/model/*`: `TableModel<C>`, `TableDomAdapter`, `SelenideTableQuery`, row/column/table assertions, controls, typed refs, `RowData`, `RowConditions`, `ExpectedValue`, exceptions | `TableLayout`, `Table`, `TableRow`, `TableColumnException`, native Selenide conditions and actions |
| `elements/base/BaseTable`                                                                                                                                                             | none                                                                                               |
| `ex/TableRowException`                                                                                                                                                                | Selenide `ElementNotFound`                                                                         |
