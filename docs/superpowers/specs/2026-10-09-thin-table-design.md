# Thin Selenide table helper — design

Status: draft for review. Date: 2026-10-09. Module: `integrations/selenide`.

## Context

`integrations/selenide` ships about 3,900 lines of table code in 62 files: a legacy stack
(`table/classic`, `table/horizontal`, `elements/base/BaseTable`) built on top of a newer model
(`table/model`: adapters, queries, assertions, controls, seven exception types).

PR #726 rewrote all table browser tests (73 of 74 scenarios) using only Selenide's public API
(`byRole`, `findBy` / `filterBy`, collection conditions). It showed that waiting, laziness,
remount safety, ARIA and div grids, assertions and cell actions need no table code at all.
It also showed three things that plain Selenide does **not** give and that the tests had to
re-implement by hand:

1. **Column by displayed header.** `columnIndex()` was copied into 5 test classes.
2. **Row lookup scoped to one column.** `findBy(text("Austria"))` matches any cell, so a value in
   a neighbouring column selects the wrong row. Tests fell back to `Condition.match` over raw
   `findElements("./td").get(col)`.
3. **Header ambiguity.** `texts().indexOf(header)` silently picks the first of duplicate headers.

## Goal

Replace the whole table stack with a small helper that covers exactly these three gaps and
returns ordinary `SelenideElement` / `ElementsCollection` for everything else.

Success criteria:

- Public table API is at most 5 types and roughly 250 lines of production code.
- Every scenario in the PR #726 table tests can be written with the new API or plain Selenide.
- No copies of `columnIndex()` or column-scoped `Condition.match` remain in tests.
- `./gradlew :integrations:selenide:check` and the table browser tests pass on the local `web`
  profile.

## Non-goals

- Table-specific assertions or conditions, typed cell values, value formatters.
- Wrappers for controls inside cells (input, select, checkbox, radio, contenteditable).
- Sorting, filtering, pagination, virtual scrolling.
- `rowspan` / `colspan`: only physical DOM cells are addressed.
- Page-factory injection (`@FindBy` on a table field).
- Changes in other modules. `NumberFormatter` and `LocalDateUtils` in `common-utils/core` are
  public API of that module and stay, even though only the removed table code used them.

## Compatibility

This is a **breaking change**, accepted for the 0.x line. The release commit uses
`feat(selenide)!:` with a `BREAKING CHANGE:` footer so release-please bumps the version.
There is no deprecation period and no bridge to the old types.

## Public API

Package `dev.quokkify.elements.table`.

### `TableLayout`

Immutable record describing the DOM shape with three relative locators:

| Component | Relative to | Meaning |
|---|---|---|
| `rows` | table root | data rows only, header row excluded |
| `cells` | a row | cells of that row, in column order |
| `headers` | table root | header cells, in column order |

Factories:

| Factory | rows | cells | headers |
|---|---|---|---|
| `html()` | `./tbody/tr[td]` | `./td \| ./th` | `./thead/tr/th` |
| `aria()` | `.//*[@role='row'][*[@role='cell' or @role='gridcell']]` | `./*[@role='cell' or @role='gridcell' or @role='rowheader']` | `.//*[@role='columnheader']` |
| `of(By rows, By cells, By headers)` | caller-defined | caller-defined | caller-defined |

`html()` uses direct-child XPath, so rows and cells of nested tables are never counted. Tables
whose header row sits inside `<tbody>` (no `<thead>`) use `of(...)`.
`aria()` matches explicit `role` attributes only (implicit roles of `<table>` markup are covered
by `html()`). Its row and header locators are descendant-based, so they also match nested ARIA
grids; this is documented, and callers with nested ARIA grids use `of(...)`. A div grid such as the current "flex" table is
expressed with `of(...)`, for example
`of(By.cssSelector(":scope > .flex-table-row:not(:first-child)"), By.cssSelector(":scope > div"),
By.cssSelector(":scope > .flex-table-row:first-child > div"))`.

### `Table`

```java
Table customers = Table.of($("#customers"), TableLayout.html());

ElementsCollection headers();
ElementsCollection rows();
TableRow row(int index);                       // 0-based among layout rows
TableRow row(String column, String value);     // first row whose cell in `column` has exact text `value`
ElementsCollection rows(String column, String value);  // all such rows
ElementsCollection column(String header);      // cells of that column across rows
SelenideElement root();
```

### `TableRow`

```java
SelenideElement cell(String header);
SelenideElement cell(int index);
ElementsCollection cells();
SelenideElement self();
```

### `HorizontalTable`

Key/value tables where each `<tr>` holds a `<th>` label and a `<td>` value.

```java
HorizontalTable contact = HorizontalTable.of($("#horizontal-customers"));
SelenideElement value(String header);   // td of the row whose th has exact text `header`
ElementsCollection headers();           // all th
```

### `TableColumnException`

Unchecked exception thrown when a header cannot be resolved to exactly one column. The message
names the requested header, the reason (`not found` or `ambiguous`), the table root and the list
of displayed headers.

## Behaviour

### Laziness and remount safety

- `Table`, `TableRow` and every returned `SelenideElement` / `ElementsCollection` hold locators,
  not `WebElement`s. A captured reference keeps working after the table root is replaced.
- `row(column, value)` is built as `rows().findBy(condition)`. The condition resolves the column
  index **inside each evaluation**, so the lookup survives header reordering and waits for
  delayed rows through Selenide's normal `should*` timeout.
- `rows(column, value)` is `rows().filterBy(condition)` with the same condition.

### Column resolution

- A header is resolved by **exact** text after Selenide's usual whitespace trimming, case
  sensitive.
- `TableRow.cell(header)` and `Table.column(header)` resolve the column index when they are
  called, waiting for the headers with Selenide's default timeout. The returned element or
  collection is then lazy by index. Reordering columns after `cell(header)` was called is not
  tracked; callers call `cell(header)` again.
- Zero matching headers → `TableColumnException` (`not found`).
  More than one → `TableColumnException` (`ambiguous`).

### Rows

- `row(column, value)` returns the first match in DOM order. Uniqueness is checked explicitly by
  the caller: `rows(column, value).shouldHave(size(1))`.
- A missing row surfaces when the returned element is used, as Selenide `ElementNotFound`, whose
  message contains the condition description `Company = "Ernst Handel"`.
- Hidden rows and cells take part in indexes, like any `ElementsCollection`. Their text follows
  Selenide (`getText` semantics); this is documented.
- A row with fewer cells than the resolved column index does not match the row condition.

### Horizontal tables

- `value(header)` is `$$("tr").findBy(th has exact text header).$("td")` relative to the root:
  lazy, waits through `should*`, and fails with `ElementNotFound` when the label is missing.
- Duplicate labels resolve to the first row; no ambiguity check (key/value tables are not
  expected to repeat labels).

## Removed

| Removed | Replacement |
|---|---|
| `table/classic/*` (10 files): `Table`, `DynamicTable`, `FlexTable`, `SelenideDataTable`, `Row`, `Cell`, bases | `Table` + `TableLayout.html()` / `of(...)` |
| `table/horizontal/*` (5 files) | `HorizontalTable` |
| `table/model/*` (44 files): adapters, query layer, row/column/table assertions, controls, typed refs, `RowData`, `RowConditions`, `ExpectedValue`, exceptions | `TableLayout`, `Table`, `TableRow`, `TableColumnException`, native Selenide conditions and actions |
| `elements/base/BaseTable` | — |
| `ex/TableRowException` | Selenide `ElementNotFound` |

`elements/base/Component` stays: dropdown components use it.

## Tests

- **Delete:** `RowConditionsContractTest` and `elements/table/model/SelenideRowConditionSnapshotTest`
  (they test removed code).
- **Migrate to `Table` where it removes hand-written helpers:** `UiTableTest`, `TableRowWaitTest`,
  `TableModelContractTest`, `TableQueryContractTest`, `TableAssertionsActionsContractTest`,
  `UiHorizontalTableTest`, `ReproHorizontalAsyncTest`. All local `columnIndex()` copies and the
  column-scoped `Condition.match` helper go away. Test method names and `@TmsLink`s stay.
- **Migrate page objects, steps and verifications:** `DelayedTablePage`, `LateMountingTablePage`,
  `HtmlTablesPage`, `HtmlHorizontalTablePage`, `HtmlTablesPageSteps`,
  `HtmlHorizontalTablePageSteps`, `HtmlTablesPageVerification`,
  `HtmlHorizontalTablePageVerification`. Fields become `Table.of(...)` initialisers instead of
  `@FindBy` table fields.
- **New `TableTest`** on existing fixtures (`table/delayed-table.html`,
  `table-model-contract/*.html`), one test per behaviour:
  - row lookup matches only the given column (value present in another column is ignored);
  - `rows(column, value)` with two matches has size 2;
  - missing header and duplicate header throw `TableColumnException` with the header list;
  - delayed row is found within the `should*` timeout;
  - captured `TableRow` survives a table remount;
  - `row(column, value)` survives header reordering;
  - `html()` ignores rows of a nested table;
  - `aria()` reads the ARIA grid fixture;
  - `of(...)` reads the custom div grid and the flex table;
  - `HorizontalTable.value` waits for a delayed label and fails with `ElementNotFound` for a
    missing one.
- **Unit tests** (no browser) for `TableLayout` factories and `TableColumnException` messages.
- Build config: the `tableModelContractStability` Gradle task keeps its filter names, since the
  three referenced test methods keep their names.

## Documentation

- Rewrite `docs/table-api.md` around the five types with the examples above.
- Replace the "Table DOM model" section in `integrations/selenide/README.md` with a short usage
  section and a link to `docs/table-api.md`.
- Update `integrations/selenide/docs/AUDIT.md` and `docs/selenide-upstream-evaluation-prompt.md`
  where they name removed types.

## Delivery

- Branch `feat/selenide-thin-table`, stacked on `test/selenide-table-recipes` (PR #726).
- Pull request in `quokkify/q4j` only. Title and squash commit:
  `feat(selenide)!: replace table stack with thin Table helper`, body with `BREAKING CHANGE:`
  listing removed packages and the replacement table above.
