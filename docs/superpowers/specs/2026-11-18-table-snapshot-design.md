# Table snapshot: bulk read in one browser call

Issue: quokkify/q4j#895. Module: `integrations/selenide`, package `dev.quokkify.elements.table`.

## Intent

Reading a large table through the lazy API (`Table.rows()` + `TableRow.cell(header)`) costs about ten
WebDriver round trips per row: `TableRow.cell(header)` calls `Table.columnIndex(header)` (a
`headers.shouldHave(...)` plus `headers.texts()`) on every call, and every lazy element re-resolves root,
row and cell. A ~400-row ARIA grid takes 28–32 s; a single `executeJavaScript` prototype reads 1219 rows in
30 ms. Tests that verify a whole table's data (for example every row's name and its list of property chips)
are unusable today.

`Table.snapshot()` reads headers, every row's cell texts and, optionally, the multi-value items inside each
cell (chips, tags) in exactly one browser call, and returns an immutable value the caller asserts on with
AssertJ.

## Goals

- One `executeJavaScript` call per snapshot, independent of row count.
- Header lookup on the snapshot follows the existing rules: `ColumnResolver` normalization, `TableColumnException`
  with `not found` / `ambiguous`.
- Multi-value cells read through a caller-supplied locator, not by splitting text.
- Text semantics match the lazy API: a hidden cell or item reads as `""` (lazy API reads hidden cells through
  `getText()`, which returns `""`).
- Works with `TableLayout.html()`, `aria()` and `of(...)` with `By.xpath` and `By.cssSelector` locators.

## Non-goals

- Waiting or retrying. A snapshot is a point-in-time read; the caller waits first
  (`table.rows().shouldHave(sizeGreaterThan(0))`).
- Remount safety. The snapshot is plain data; it never touches the DOM again.
- Virtualized grids: only rows mounted in the DOM are read.
- Typed values, `<input>`/`<select>` values, `colspan`/`rowspan` (same limits as the lazy API).
- Per-column item locators.
- Speeding up the lazy `cell(header)` chain (caching the column index). Worth doing, tracked separately.

## Anti-goals

- A snapshot that disagrees with the lazy API on the same DOM (different text for the same cell, different
  rows or headers counted). That would make the two APIs untrustworthy together.
- Silent fallback to per-element WebDriver calls when a locator cannot be evaluated in JS: unsupported `By`
  types fail loudly.
- Breaking `TableLayout`'s public record constructor or any existing signature.

## Constraints

- `TableLayout` is a public record `(By rows, By cells, By headers)`; it must not change.
- Locators reach JS only as strings. `TableLayout.xpath(By)` already extracts XPath by parsing
  `By.toString()` (`"By.xpath: "` prefix); CSS uses the same approach with the `"By.cssSelector: "` prefix.
  Any other `By` type throws `UnsupportedOperationException` naming the locator.
- Selenide 7.19.1. Tests are TestNG + AssertJ against the nginx fixtures, extending `BaseTest`, like
  `TableTest`. The fixtures this design reuses (classic, nested, repeated headers, ARIA, CSS div grid) live in
  `tools/environment/assets/nginx/html/table-model-contract/` and open through
  `BaseTest.openTableModelContract(...)`; new fixtures (chip grid, 1000+ rows) go there too, with a matching
  `BaseTest` open helper (my call).
- The public surface stays small: the thin-table redesign deliberately has five public types.

## Approaches

1. **Cell as a value (chosen).** `TableSnapshot` holds `List<SnapshotRow>`; each `SnapshotRow` holds
   `List<SnapshotCell>`; `SnapshotCell(text, values)`. Addressing mirrors the lazy API (`row → cell(header)`).
   Cost: three new public types.
2. **Flat lists.** `rows()` → `List<List<String>>` plus `values()` → `List<List<List<String>>>`. One type, but
   triple nesting is unreadable and texts/values can drift apart by index. Rejected.
3. **Item locator as a fourth `TableLayout` component.** Breaks the public record constructor and forces the
   concept on every layout. Rejected.

The item locator is one `By` for the whole snapshot, not per column: cells without matches simply have empty
`values()`. A per-column `Map<header, By>` is more precise but adds complexity for no gain in the motivating
case.

## Design

### Public API

Signatures only (not compilable code):

```text
Table
  TableSnapshot snapshot()                      texts only; values() always empty
  TableSnapshot snapshot(By items)              also reads items inside each cell

record TableSnapshot(List<String> headers, List<SnapshotRow> rows)
  List<String> column(String header)            text of that column in every row
  List<List<String>> columnValues(String header) items of that column in every row

record SnapshotRow(List<SnapshotCell> cells)
  SnapshotCell cell(int index)
  SnapshotCell cell(String header)

record SnapshotCell(String text, List<String> values)
```

- All lists are unmodifiable copies (`List.copyOf`).
- `headers()` are normalized with `ColumnResolver.normalize`. Cell texts and item texts are normalized the
  same way.
- `SnapshotRow.cell(String header)` needs the headers, so `SnapshotRow` keeps a reference to its owning
  snapshot's header list. A record cannot hold a non-component field, so `SnapshotRow` is either a small
  final class with `cells()` and value-based `equals`/`hashCode` over the cells, or a record with a
  `headers` component; builder's call. `SnapshotRow` is listed as a record below only for brevity.
- Header lookup uses `ColumnResolver.indexOf(headers, header, table)` where `table` is the root's
  `toString()` captured at snapshot time, so `TableColumnException` messages match the lazy API.
- Short rows: `cell(header)` / `cell(int)` on a row with fewer cells than the index throws
  `IndexOutOfBoundsException`-style failure with a clear message (mirrors "a row with fewer cells does not
  match"); `column(header)` / `columnValues(header)` return `""` / empty list for such rows so the result stays
  aligned with `rows()`. Builder may pick the exact exception type; it must name the row index and header.

### Browser script

One static JS resource (string constant or classpath resource, builder's call), called once via
`Selenide.executeJavaScript(script, root, rowsSpec, cellsSpec, headersSpec, itemsSpec)` where each spec is
`{kind: "xpath" | "css", expr: "..."}` or `null` for items.

- `find(context, spec)`: XPath through `document.evaluate(expr, context, null,
  XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null)`; CSS through `context.querySelectorAll(expr)` (`:scope`
  works). Results in document order, elements only.
- `text(el)`: `""` when the element is not rendered (`el.getClientRects().length === 0`, or
  `checkVisibility()` returns false where available); otherwise `el.innerText`. Java side normalizes.
- Returns `{headers: string[], rows: [{texts: string[], values: string[][] | null}]}`; hidden items are
  skipped (not returned as `""`).
- The root is passed as the resolved `WebElement` of `table.root()`; if the root does not exist, Selenide's
  usual `ElementNotFound` surfaces before the script runs.

### Locator translation

A package-private helper turns `By` into a spec: `"By.xpath: "` → xpath, `"By.cssSelector: "` → css, anything
else → `UnsupportedOperationException("snapshot() supports By.xpath and By.cssSelector, got: " + by)`. It is
validated before the browser call. `TableLayout.xpath` stays as is; the helper may reuse it.

## Verification

- **One call:** a test that counts script executions (`executeScript`) for a snapshot of the large fixture
  and asserts exactly 1. Resolving the root `WebElement` (`findElement`) is not counted.
  *Implementation note:* Selenide 7.19.1 unwraps the listener-decorated driver before `executeScript`
  (`JavaScript.asJsExecutor`), so a `WebDriverListener` cannot see script calls. The test instead records every
  driver call it can see during a snapshot of a 3-row and a 1200-row grid, asserts both sequences are identical
  and contain no per-element reads (`findElements`, `getText`, `getDomProperty`, `getAttribute`), and the speed
  test bounds the time. Mechanism is the builder's call (a Selenium `WebDriverListener` registered for the test, or a
  Selenide listener); it must count real driver calls, not mocked ones.
- **Agreement with the lazy API:** on existing fixtures (`html()` classic table, nested table, repeated
  headers, ARIA grid, CSS div grid via `of(...)`), `snapshot().column(h)` equals `table.column(h).texts()`
  normalized, or the `TableRow.cell(h)` texts where `column()` is unsupported. Nested-table rows are not counted.
- **Hidden cells:** a fixture row with a hidden cell reads `""` in both APIs; a hidden chip is skipped.
- **Items:** a new fixture grid with chip cells (both flex and inline chips) returns exactly the chip labels
  via `columnValues(...)`.
- **Headers:** `column("Missing")` → `TableColumnException` `not found`; repeated header → `ambiguous`.
- **Unsupported locator:** `By.id(...)` in the layout or as items → `UnsupportedOperationException` before any
  browser call.
- **Speed:** a generated fixture with 1000+ rows reads in well under 1 s (assert a generous bound, for example
  2 s, to stay non-flaky).

## Docs

`docs/table-api.md`:
- Add `TableSnapshot`, `SnapshotRow`, `SnapshotCell` to "Public types".
- New section "Bulk read (`snapshot`)" with usage, the wait-first recipe, the item locator, hidden-element
  semantics, and the mounted-rows-only limit.
- The `### Cost` note points to it.
- "Laziness and remount safety" gets one line: a snapshot is the exception — plain data, not lazy.

## Left to the builder

- Script packaging (constant vs classpath resource).
- How `SnapshotRow` carries headers without polluting record equality.
- Exact exception type for out-of-range cells.
- The call-counting mechanism in tests.
- Fixture generation for the 1000+ row table (static file vs generated by a small inline script in the page).
