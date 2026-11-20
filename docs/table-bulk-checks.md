# Bulk read and whole-table checks

The table helper in `dev.quokkify.elements.table` (see [Table API](table-api.md)) reads a table lazily, element by
element. That costs about ten WebDriver round trips per row: a check over 400 rows took 28–32 s in issue #895. The
bulk API reads the **whole table in one browser call** and builds on that:

1. **`snapshot()`**: headers, every row's cell texts and, optionally, the items inside each cell (chips, tags), as
   plain immutable data;
2. **`shouldHave(TableCondition)`**: compares the whole table and waits like any Selenide `should*`. A mismatch is
   reported as a short diff, not 400 rows;
3. **`shouldSatisfy(assertions)`**: runs your AssertJ assertions on a fresh snapshot until they pass.

Each read costs one root lookup plus one script call, whatever the number of rows. Header lookup follows the usual
rules of the helper: exact text after whitespace normalization, and `TableColumnException` when a header is
`not found` or `ambiguous`.

The screenshots below are taken by real Selenide runs against the test fixture
`tools/environment/assets/nginx/html/table-model-contract/snapshot.html`. The examples in sections 1–9 are executed
and asserted by `TableBulkDocExamplesTest` on every test run. `./gradlew :integrations:selenide:tableDocScreenshots`
also saves these images to `build/reports/table-docs`. The error messages in section 10 are covered by the unit
tests in `dev.quokkify.elements.table`.

Each image shows the code, what it returned, and the elements involved:

- **amber** for headers or unexpected rows;
- **blue** for rows or key cells;
- **red** for items or differences;
- **green** for what a passing check compared;
- **grey dashed** for a cell with hidden items.

Rows 1 and 3 of the fixture look shifted because their Secret cell is hidden and takes no space.

## API

| Type             | Methods                                                                                                                                        |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `Table`          | `snapshot()`, `snapshot(By items)`, `shouldHave(TableCondition[, Duration])`, `shouldSatisfy([By items,] Consumer<TableSnapshot>[, Duration])` |
| `TableSnapshot`  | `headers()`, `rows()`, `column(header)`, `columnItems(header)`, `columnBy(keyHeader, header)`, `columnItemsBy(keyHeader, header)`              |
| `SnapshotRow`    | `cells()`, `cell(int index)`, `cell(String header)`                                                                                            |
| `SnapshotCell`   | `text()`, `items()` (texts of the items found inside the cell; empty without an items locator)                                                 |
| `TableCondition` | `exactRows(List<String> headers, List<List<String>> rows)`, `exactItemsBy(keyHeader, header, By items, Map<String, List<String>>)`             |

All examples use this ARIA grid, where each **Properties** cell holds a list of chips:

```java
Table grid = Table.of($("#chip-grid"), TableLayout.aria());
By chip = By.cssSelector(".chip");
```

## 1. Read the whole table once

```java
TableSnapshot snap = grid.snapshot();
snap.headers();         // [Name, Properties, Secret]
snap.column("Name");    // [Alpha, Beta, Gamma]
snap.rows().get(1).cell("Secret").text();   // s2
```

A snapshot is a point-in-time read. It waits for the table root to exist, but not for rows. To verify data, use the
checks in sections 5–8, which wait.

![snapshot](table-bulk-checks/01-snapshot.png)

## 2. Items inside a cell (chips, tags)

Pass a locator for the items. It is evaluated inside every cell, in the same browser call:

```java
TableSnapshot snap = grid.snapshot(chip);
snap.columnItems("Properties");   // [[Red, Blue], [Green], [Violet]]
```

Do not split `text()` on line breaks. Whether chips are separated by `\n` or by spaces depends on the page's CSS
(flex or inline), and the item locator does not depend on it. An XPath items locator must be relative to the cell:
start it with `./` or `.//`. A locator starting with `//` matches every chip of the page in every cell.

![items](table-bulk-checks/02-items.png)

## 3. Hidden elements read like `getText()`

Visibility is decided by Selenium's own `isDisplayed` atom, the one `WebElement.isDisplayed()` runs. After whitespace
normalization, a snapshot therefore reads the same text as the lazy API's `getText()`:

- **Hidden cells.** A hidden cell reads as `""`: `display: none` or `hidden`, `visibility: hidden`, `opacity: 0`,
  zero size, or clipped by an `overflow: hidden` ancestor.
- **Hidden items.** Hidden items are skipped.
- **Hidden text.** Text of hidden descendants is left out of a cell's text. A visible child of a
  `visibility: hidden` parent still counts.
- **Icons and styling.** SVG icons contribute no text, and `text-transform` is applied.
- **Selects.** A `<select>` reads as the text of its options.

```java
grid.snapshot(chip).column("Secret");   // ["", "s2", ""]: row 1 display:none, row 3 opacity:0
```

![hidden](table-bulk-checks/03-hidden.png)

## 4. A column keyed by another column

```java
Map<String, List<String>> chips = grid.snapshot(chip).columnItemsBy("Name", "Properties");
// {Alpha=[Red, Blue], Beta=[Green], Gamma=[Violet]}

Map<String, String> secrets = grid.snapshot().columnBy("Name", "Secret");
// {Alpha=, Beta=s2, Gamma=}
```

The maps keep row order and are unmodifiable. Keys must be unique: a row with no key cell, or a repeated key, throws
`IllegalStateException` naming the rows.

![keyed map](table-bulk-checks/04-keyed-map.png)

## 5. Check the whole table, with waiting

```java
grid.shouldHave(TableCondition.exactItemsBy("Name", "Properties", chip, Map.of(
    "Alpha", List.of("Red", "Blue"),
    "Beta", List.of("Green"),
    "Gamma", List.of("Violet"),
    "Delta", List.of("Gold", "Grey"))));
```

For this example the test adds a fourth row, Delta, after the page has opened; the fixture itself has three rows.
Every attempt reads the table again, and the check passes as soon as the data matches. No separate
`rows().shouldHave(...)` wait is needed.

- **Order.** Keys may come in any row order; items within a cell are compared in displayed order.
- **Timeout.** The default is Selenide's timeout; pass a `Duration` as the second argument to change it.
- **Failure.** A timed-out check throws a Selenide `ElementShould` on the table root, or `ElementNotFound` when the
  root never appears, with the usual screenshot, report step and soft-assert support.

Left: an attempt before Delta is rendered. Right: the check has passed.

<img src="table-bulk-checks/05a-wait-before.png" alt="first attempt" width="420"> <img src="table-bulk-checks/05b-wait-after.png" alt="check passed" width="420">

## 6. A failure you can read on 400 rows

The failure message lists only the differences, at most 10 per section, followed by `... N more`:

- missing keys, in the order of the expected map;
- unexpected keys and changed values, in row order.

`Map.of` has no defined order, so pass a `LinkedHashMap` if you want missing keys listed in a stable order.

```java
grid.shouldHave(TableCondition.exactItemsBy("Name", "Properties", chip, Map.of(
    "Alpha", List.of("Red"), "Beta", List.of("Green"), "Gamma", List.of("Violet"),
    "Epsilon", List.of())), Duration.ofMillis(300));
```

```
Actual value: 4 rows, expected 4
missing (1):
  Epsilon
unexpected (1):
  Delta
different (1):
  Alpha: expected [Red] but was [Red, Blue]
```

The first line compares row counts only; equal counts do not mean the rows match.

Like any Selenide `should*` on an element, the message also contains Selenide's description of the table root,
including its whole text, after the diff. Selenide builds that description on every failed attempt, which costs a
few extra WebDriver calls per poll. A passing attempt costs one script call.

![failure diff](table-bulk-checks/06-failure-diff.png)

## 7. Several columns, row by row

`exactRows` compares the texts of the given columns row by row, in row order. A cell missing from a short row reads
as `""`, the same as an empty cell.

```java
grid.shouldHave(TableCondition.exactRows(List.of("Name", "Secret"), List.of(
    List.of("Alpha", ""), List.of("Beta", "s2"), List.of("Gamma", ""), List.of("Delta", "s4"))));
```

Rows are compared by position, so one inserted row shows up as many `different` rows. The `Actual value:` line of
the failure gives both row counts.

![exact rows](table-bulk-checks/07-exact-rows.png)

## 8. Any check with AssertJ

For anything other than whole-table equality, write ordinary AssertJ assertions over the snapshot. They are re-read
and re-run until they pass. This example runs on the original three rows:

```java
grid.shouldSatisfy(chip, snap -> {
  assertThat(snap.column("Name")).doesNotContain("Failed").hasSize(3);
  assertThat(snap.columnItemsBy("Name", "Properties").get("Alpha")).contains("Red");
});
```

These failures count as "not ready yet" and are retried:

- a failed assertion;
- a header that is not displayed or is ambiguous;
- a row or cell that is not rendered (`IndexOutOfBoundsException`, `NoSuchElementException`);
- a key column with a missing cell or a repeated key.

Any other exception fails the check at once, as itself, so a bug in the test surfaces immediately instead of waiting
out the timeout. For example, `Integer.parseInt` on a cell that still shows "Loading…" throws
`NumberFormatException`. Assert on the text first, for example `assertThat(text).matches("\\d+")`, and parse only
after that. Likewise, `columnBy(...).get(key)` returns `null` for a key that is not rendered yet. Pass it to
`assertThat(...)`, which is retried, and do not dereference it: a `NullPointerException` is not retried.

On a timeout the actual value is the last AssertJ message. For whole-table equality on large tables, prefer a
`TableCondition` and its short diff.

![should satisfy](table-bulk-checks/08-should-satisfy.png)

## 9. Large tables

```java
Map<String, String> values = Table.of($("#large-grid"), TableLayout.aria())
    .snapshot().columnBy("Name", "Value");   // 1200 rows
```

1200 rows are read in one browser call. The screenshot shows the time measured on that run. Reading the same data
row by row through `TableRow.cell(header)` costs about ten round trips per row.

![large grid](table-bulk-checks/09-large-grid.png)

## 10. Errors

`{...}` is the locator of the table root. The last three lines use a hypothetical table `#t`.

```
snap.column("Region")                → TableColumnException: Column "Region" not found in {#chip-grid}; displayed headers: [Name, Properties, Secret]
snap.column("Company")               → TableColumnException: Column "Company" ambiguous in {#repeated-table}; displayed headers: [Country, Company, Company]
snap.rows().get(1).cell("Secret")    → NoSuchElementException: row 1 has 1 cells, no cell for header "Secret" in {#t}   (short row)
snap.columnItemsBy("Name", "Tags")   → IllegalStateException: rows 0 and 2 both have "Alpha" in key column "Name" of {#t}
snapshot() with By.id / By.name      → UnsupportedOperationException: snapshot() supports By.xpath and By.cssSelector, got: By.id: r
```

When used inside `shouldHave` or `shouldSatisfy`, the first four fail the attempt and are retried. After the timeout,
the `ElementShould` message contains their text; inside `shouldSatisfy` the text is prefixed with the exception
class. An unsupported locator, or an XPath the browser cannot parse, fails at once without waiting.

## Which one to use

| You want to                                               | Use                                                              |
| --------------------------------------------------------- | ---------------------------------------------------------------- |
| check one column of an XPath layout, with waiting         | `table.column(h).shouldHave(exactTexts(...))` (already one call) |
| check a whole table: key column to items, or aligned rows | `table.shouldHave(TableCondition.exactItemsBy / exactRows(...))` |
| any other check over the data, with waiting               | `table.shouldSatisfy(snap -> assertThat(...))`                   |
| read the data once, for custom logic                      | `table.snapshot()` / `snapshot(By items)`                        |
| act on a row or cell (click, type), or a small table      | the lazy API: `row(column, value).cell(header)`                  |

`column(h).texts()` reads hidden cell text too (Selenide reads `innerText || textContent` without a visibility
check), while a snapshot reads hidden cells as `""`.

## Matching rules and limitations

- Texts and headers are whitespace-normalized: runs of whitespace and NBSP collapse to one space, and the result is
  trimmed. Write expected texts with single spaces.
- Locators of the layout and the items locator must be `By.xpath` or `By.cssSelector`.
- Only rows mounted in the DOM are read. A virtualized grid returns the rows currently rendered.
- Values are read as visible text: an `<input>` reads as `""`, as with `getText()`.
- `colspan` / `rowspan` are not modelled, as in the rest of the helper.
