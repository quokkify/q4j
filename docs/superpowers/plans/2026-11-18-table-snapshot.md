# Table snapshot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `Table.snapshot()` / `Table.snapshot(By items)` read headers, cell texts and per-cell items of a table in exactly one browser call and return immutable data.

**Architecture:** A package-private `SnapshotLocator` turns each `By` into an `{kind, expr}` spec; one JS script evaluates the layout's locators relative to the root and returns plain maps/lists; `TableSnapshot.fromScript(...)` parses that into `TableSnapshot` → `SnapshotRow` → `SnapshotCell`. Header lookup reuses `ColumnResolver`.

**Tech Stack:** Java 21, Selenide 7.19.1, TestNG, AssertJ, nginx fixtures.

**Spec:** `docs/superpowers/specs/2026-11-18-table-snapshot-design.md`

## Global Constraints

- Package `dev.quokkify.elements.table`, module `integrations/selenide`. Do not change `TableLayout`'s record components or any existing public signature.
- New public types: exactly `TableSnapshot`, `SnapshotRow`, `SnapshotCell`. Everything else package-private.
- Exactly one `executeScript` per snapshot, regardless of row count. Root resolution (`findElement`) is not counted.
- Supported locators: `By.xpath` and `By.cssSelector` only; anything else throws `UnsupportedOperationException` with message `snapshot() supports By.xpath and By.cssSelector, got: <by>` before any browser call.
- Headers, cell texts and item texts are normalized with `ColumnResolver.normalize`.
- Hidden cell → text `""`; hidden item → skipped.
- All returned lists are unmodifiable.
- Java style: `Objects.isNull` / `Objects.nonNull`, not `== null`.

Commands (from the thin-table plan):

- `COMPILE` = `./gradlew :integrations:selenide:compileTestJava :integrations:selenide:checkstyleMain :integrations:selenide:checkstyleTest --console=plain` → `BUILD SUCCESSFUL`.
- `UNIT <pattern>` = `./gradlew :integrations:selenide:test --tests '<pattern>' --console=plain` (no browser).
- `BROWSER <pattern>` = `CI= BASE_URL=http://host.docker.internal:80 NGINX_BASE_URL=http://host.docker.internal:80 BROWSER_REMOTE_URL=http://localhost:4444/wd/hub ./gradlew :integrations:selenide:test --tests '<pattern>' --console=plain`. Requires `env -u CI ./tools/environment/scripts/infra/run_app.sh web` (`curl -s http://localhost:4444/wd/hub/status` shows `"ready": true`).

## Review Focus

1. **Cell with nested table inside** (`#custom-grid` row 0 contains `#nested-custom-grid`): the outer row's text includes the nested text, but the nested rows must not appear as outer rows. Pinned in Task 3 (`snapshotIgnoresNestedGridRows`).
2. **Headers not mounted yet / empty table**: `snapshot()` returns `headers() == []`, `rows() == []` without throwing; `column("X")` then throws `TableColumnException` `not found` with `[]`. Pinned in Task 1 (`columnOnEmptySnapshotIsNotFound`).
3. **XPath union in headers** (`html()` headers use `|`): document order must be preserved, so headers come out left-to-right. Pinned in Task 3 (`snapshotMatchesLazyApiOnHtmlLayout` on `#classic`).
4. **Header text with `&nbsp;` / line breaks**: `snap.column("Company Name")` matches a header rendered as `Company Name`. Pinned in Task 1 (`normalizesHeadersAndTexts`).
5. **Root that does not exist**: Selenide `ElementNotFound` surfaces (not a JS `null` error). Pinned in Task 3 (`snapshotOfMissingRootFailsWithElementNotFound`).

---

### Task 1: Snapshot value types

**Files:**

- Create: `integrations/selenide/src/main/java/dev/quokkify/elements/table/SnapshotCell.java`
- Create: `integrations/selenide/src/main/java/dev/quokkify/elements/table/SnapshotRow.java`
- Create: `integrations/selenide/src/main/java/dev/quokkify/elements/table/TableSnapshot.java`
- Test: `integrations/selenide/src/test/java/dev/quokkify/elements/table/TableSnapshotParsingTest.java`

**Interfaces:**

- Consumes: `ColumnResolver.normalize(String)`, `ColumnResolver.normalize(List<String>)`, `ColumnResolver.indexOf(List<String>, String, String)`, `TableColumnException`.
- Produces:
  - `public record SnapshotCell(String text, List<String> values)` — compact constructor: `requireNonNull`, `values = List.copyOf(values)`.
  - `public final class SnapshotRow` — `List<SnapshotCell> cells()`, `SnapshotCell cell(int index)`, `SnapshotCell cell(String header)`; package-private constructor `SnapshotRow(int index, List<SnapshotCell> cells, List<String> headers, String table)`; `equals`/`hashCode`/`toString` over `cells` only.
  - `public final class TableSnapshot` — `List<String> headers()`, `List<SnapshotRow> rows()`, `List<String> column(String header)`, `List<List<String>> columnValues(String header)`; package-private `static TableSnapshot fromScript(Map<?, ?> result, String table)` where `result` is `{"headers": List<String>, "rows": List<Map{"texts": List<String>, "values": List<List<String>> | null}>}`. Normalization happens here.

- [ ] **Step 1: Write the failing tests** in `TableSnapshotParsingTest` (TestNG + AssertJ), building input with a small helper `result(List<String> headers, List<Map<String, Object>> rows)`:
  - `readsHeadersRowsAndCells`: headers `["Country","Company"]`, rows `[["Austria","Alfreds"],["Germany","Berglunds"]]` → `headers()` equals input; `rows().get(1).cell("Company").text()` = `"Berglunds"`; `rows().get(0).cell(0).values()` is empty.
  - `normalizesHeadersAndTexts`: header `" Company  Name \n"`, cell `"  Alfreds \n Futterkiste "`, value `" Chip A "` → `column("Company Name")` = `["Alfreds Futterkiste"]`, `columnValues("Company Name")` = `[["Chip A"]]`.
  - `columnOfMissingHeaderIsNotFound`: `column("Region")` → `TableColumnException` with message containing `not found` and `#t` (table arg `"#t"`).
  - `columnOfRepeatedHeaderIsAmbiguous`: headers `["Company","Company"]` → `column("Company")` → `TableColumnException` containing `ambiguous`.
  - `columnOnEmptySnapshotIsNotFound`: headers `[]`, rows `[]` → `rows()` empty; `column("X")` → `TableColumnException` containing `not found` and `[]`.
  - `shortRowAlignsColumnButCellFails`: headers `["Country","Company"]`, rows `[["Austria","Alfreds"],["Germany"]]` → `column("Company")` = `["Alfreds",""]`; `columnValues("Company")` = `[[],[]]`; `rows().get(1).cell("Company")` → `NoSuchElementException` whose message contains `row 1`, `Company` and `#t`.
  - `valuesNullMeansEmpty`: row map with `"values": null` → every cell's `values()` is empty.
  - `listsAreUnmodifiable`: `headers().add("x")`, `rows().add(null)`, `rows().get(0).cells().add(null)`, `cell(0).values().add("x")` each throw `UnsupportedOperationException`.
  - `rowEqualityIgnoresHeaders`: two `SnapshotRow`s with equal cells but different header lists are `equal` with equal `hashCode`.

- [ ] **Step 2: Run** `UNIT 'dev.quokkify.elements.table.TableSnapshotParsingTest'` → compilation FAIL (`cannot find symbol TableSnapshot`).

- [ ] **Step 3: Implement** the three types per Interfaces. `SnapshotRow.cell(String)` resolves the index with `ColumnResolver.indexOf(headers, header, table)`, then throws `NoSuchElementException("row " + index + " has " + cells.size() + " cells, no cell for header \"" + header + "\" in " + table)` when out of range. `cell(int)` throws `NoSuchElementException` with the row index and the requested index when out of range. `column`/`columnValues` resolve the index once and map each row to `""` / `List.of()` when the row is short.

- [ ] **Step 4: Run** the same `UNIT` command → PASS; then `COMPILE` → `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add integrations/selenide/src/main/java/dev/quokkify/elements/table/{SnapshotCell,SnapshotRow,TableSnapshot}.java \
        integrations/selenide/src/test/java/dev/quokkify/elements/table/TableSnapshotParsingTest.java
git commit -m "feat(selenide): add TableSnapshot value types"
```

### Task 2: Locator translation

**Files:**

- Create: `integrations/selenide/src/main/java/dev/quokkify/elements/table/SnapshotLocator.java`
- Test: `integrations/selenide/src/test/java/dev/quokkify/elements/table/SnapshotLocatorTest.java`

**Interfaces:**

- Consumes: `TableLayout.xpath(By)` (may reuse).
- Produces: package-private `record SnapshotLocator(String kind, String expr)` with `static SnapshotLocator of(By by)` (`kind` is `"xpath"` or `"css"`) and `Map<String, String> toScriptArg()` returning `{"kind": kind, "expr": expr}`.

- [ ] **Step 1: Write the failing tests:**
  - `translatesXpath`: `of(By.xpath("./tbody/tr[td]"))` = `new SnapshotLocator("xpath", "./tbody/tr[td]")`.
  - `translatesXpathUnion`: the `html()` headers XPath (with `|`) is kept verbatim.
  - `translatesCss`: `of(By.cssSelector(":scope > .data-row"))` = `("css", ":scope > .data-row")`.
  - `rejectsOtherBy`: `of(By.id("x"))` and `of(By.className("x"))` → `UnsupportedOperationException` with message `snapshot() supports By.xpath and By.cssSelector, got: By.id: x`.
  - `scriptArgShape`: `toScriptArg()` = `Map.of("kind","css","expr",".chip")`.

- [ ] **Step 2: Run** `UNIT 'dev.quokkify.elements.table.SnapshotLocatorTest'` → compilation FAIL.

- [ ] **Step 3: Implement** by prefix match on `by.toString()`: `"By.xpath: "` and `"By.cssSelector: "`.

- [ ] **Step 4: Run** the `UNIT` command → PASS; `COMPILE` → `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit** — `feat(selenide): translate snapshot locators`.

### Task 3: `Table.snapshot()` and the browser script

**Files:**

- Create: `integrations/selenide/src/main/resources/dev/quokkify/elements/table/snapshot.js`
- Modify: `integrations/selenide/src/main/java/dev/quokkify/elements/table/Table.java` (add two public methods after `rows(String, String)`)
- Test: `integrations/selenide/src/test/java/dev/quokkify/elements/table/TableSnapshotLocatorValidationTest.java` (no browser)
- Test: `integrations/selenide/src/test/java/dev/quokkify/test/TableSnapshotTest.java` (browser)

**Interfaces:**

- Consumes: `SnapshotLocator.of(By)`, `SnapshotLocator.toScriptArg()`, `TableSnapshot.fromScript(Map<?, ?>, String)`.
- Produces: `public TableSnapshot snapshot()` (= `snapshot(null)` internally, items spec `null`) and `public TableSnapshot snapshot(By items)` (`items` non-null, `requireNonNull(items, "items")`).

Script contract: `arguments = [root, rowsSpec, cellsSpec, headersSpec, itemsSpec|null]`. `find(context, spec)`: XPath via `document.evaluate(expr, context, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null)`, CSS via `context.querySelectorAll(expr)`; keep element nodes only. Hidden test: `el.getClientRects().length === 0` (or `el.checkVisibility && !el.checkVisibility()`). `text(el)` = hidden ? `""` : `el.innerText`. Returns `{headers: find(root, headers).map(text), rows: find(root, rows).map(r => ({texts: cells.map(text), values: items ? cells.map(c => find(c, items).filter(visible).map(text)) : null}))}`. Load the resource once into a `private static final String` (classpath read, fail with `IllegalStateException` if missing).

- [ ] **Step 1: Write the failing no-browser test** `TableSnapshotLocatorValidationTest.rejectsUnsupportedLocatorBeforeBrowserCall`: `Table.of($("#t"), TableLayout.of(By.id("r"), By.xpath("./td"), By.xpath("./th"))).snapshot()` → `UnsupportedOperationException` containing `By.id: r`; same for `Table.of($("#t"), TableLayout.html()).snapshot(By.name("chip"))`. No browser is open in a `UNIT` run, so passing proves validation precedes root resolution.

- [ ] **Step 2: Write the failing browser tests** in `dev.quokkify.test.TableSnapshotTest extends BaseTest`:
  - `snapshotMatchesLazyApiOnHtmlLayout`: `openClassicVariantsFixture()`; `Table t = Table.of($("#classic"), TableLayout.html())`; `snap.headers()` = `t.headers().texts()` normalized (`["Country","Company"]`); `snap.column("Company")` = `t.column("Company").texts()`.
  - `snapshotMatchesLazyApiOnAriaGrid`: `openCustomGridsFixture()`; `#aria-grid` with `TableLayout.aria()` → headers `["Country","Company"]`, `column("Company")` = `["Alfreds"]`.
  - `snapshotIgnoresNestedGridRows`: `openCustomGridsFixture()`; `#custom-grid` with the CSS layout `TableModelContractTest.grid(...)` uses (`:scope > .data-row`, the same cells locator, `:scope > .header-row > .cell:not([hidden])`) → `rows()` size 3; `rows().get(2).cells()` size 1; `rows().get(1).cell("Company").text()` = `""`; `rows().get(0).cell("Company").text()` starts with `"Outer"`. For each row index i < 2 and header `Country`, `snap.rows().get(i).cell("Country").text()` equals `ColumnResolver`-normalized `t.row(i).cell("Country").text()`.
  - `snapshotDetectsAmbiguousHeader`: `openEdgeCasesFixture()`; `#repeated-table` with `html()` → `snap.column("Company")` throws `TableColumnException` containing `ambiguous`.
  - `snapshotOfMissingRootFailsWithElementNotFound`: `Table.of($("#no-such-table"), TableLayout.html()).snapshot()` → `ElementNotFound`.

  Move the CSS grid layout to a shared test helper only if both test classes need it; otherwise declare it locally.

- [ ] **Step 3: Run** `UNIT 'dev.quokkify.elements.table.TableSnapshotLocatorValidationTest'` and `BROWSER 'dev.quokkify.test.TableSnapshotTest'` → compilation FAIL (`cannot find symbol snapshot`).

- [ ] **Step 4: Implement** `snapshot(...)`: build the four specs via `SnapshotLocator` first, then `Selenide.executeJavaScript(SCRIPT, root.toWebElement(), ...)` — `toWebElement()` raises `ElementNotFound` for a missing root — then `TableSnapshot.fromScript(result, root.toString())`.

- [ ] **Step 5: Run** both commands from Step 3 → PASS; `COMPILE` → `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit** — `feat(selenide): read a table in one browser call with snapshot()`.

### Task 4: Items, hidden elements, one call, large table

**Files:**

- Create: `tools/environment/assets/nginx/html/table-model-contract/snapshot.html`
- Modify: `integrations/selenide/src/test/java/dev/quokkify/test/BaseTest.java` (add `protected static void openSnapshotFixture()` → `openTableModelContract("snapshot.html")`)
- Modify: `integrations/selenide/src/test/java/dev/quokkify/test/TableSnapshotTest.java`

**Interfaces:**

- Consumes: `Table.snapshot(By)`, `TableSnapshot.columnValues(String)`.
- Produces: fixture ids `#chip-grid`, `#large-grid`.

Fixture `snapshot.html` (same page skeleton as `custom-grids.html`):

- `#chip-grid`, ARIA roles (`role=grid/row/columnheader/gridcell`), visible headers `Name`, `Properties`, `Secret`. Rows:
  1. `Alpha` | cell with `display:flex` holding `<span class="chip">Red</span><span class="chip">Blue</span>` | cell with `style="display:none"` and text `s1`.
  2. `Beta` | inline chips `<span class="chip">Green</span> <span class="chip" hidden>Ghost</span>` | `s2` (visible).
  3. `Gamma` | empty | empty.
- `#large-grid`, ARIA roles, headers `Name`, `Value`, rows generated on load by an inline script: 1200 rows, row _i_ (0-based) = `Name i`, `Value i`.

- [ ] **Step 1: Write the failing tests:**
  - `readsChipsWithItemLocator`: `openSnapshotFixture()`; `Table.of($("#chip-grid"), TableLayout.aria()).snapshot(By.cssSelector(".chip"))` → `column("Name")` = `["Alpha","Beta","Gamma"]`; `columnValues("Properties")` = `[["Red","Blue"],["Green"],[]]` (inline chips and the hidden `Ghost` prove locator-based reading).
  - `hiddenCellReadsEmptyInBothApis`: `snap.rows().get(0).cell("Secret").text()` = `""` and `t.row(0).cell("Secret").text()` = `""`; `rows().get(1).cell("Secret").text()` = `"s2"`.
  - `snapshotWithoutItemsHasNoValues`: `snapshot()` on `#chip-grid` → every cell's `values()` empty.
  - `snapshotIsOneScriptCall`: register a Selenium `WebDriverListener` counting `beforeExecuteScript` (via `WebDriverRunner.addListener(...)` before `openSnapshotFixture()`; remove/close in `finally`); wait `t.rows().shouldHave(size(1200))`; reset the counter; `snapshot()`; assert counter = 1 and `rows()` size 1200.
  - `largeTableReadsFast`: on `#large-grid` after the same wait, time `snapshot()` with `System.nanoTime()` → under `Duration.ofSeconds(2)`; `column("Value").get(1199)` = `"Value 1199"`.

- [ ] **Step 2: Run** `BROWSER 'dev.quokkify.test.TableSnapshotTest'` → the new tests FAIL (fixture missing / `openSnapshotFixture` not found).

- [ ] **Step 3: Add** the fixture and the `BaseTest` helper. If any test still fails, fix `snapshot.js` (Task 3 file), not the expectation.

- [ ] **Step 4: Run** `BROWSER 'dev.quokkify.test.TableSnapshotTest'` → all PASS; `COMPILE` → `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit** — `test(selenide): cover snapshot items, hidden cells and call count`.

### Task 5: Documentation

**Files:**

- Modify: `docs/table-api.md`

- [ ] **Step 1: Edit** `docs/table-api.md`:
  - "Public types" line: add `TableSnapshot`, `SnapshotRow`, `SnapshotCell`.
  - `### Table` method table: add rows `snapshot()` — "headers and every row's cell texts, read in one browser call" and `snapshot(By items)` — "same, plus texts of `items` inside each cell".
  - New section `## Bulk read (snapshot)` after "Behaviour and limits": usage example (`table.rows().shouldHave(sizeGreaterThan(0)); TableSnapshot snap = table.snapshot(By.cssSelector(".chip")); assertThat(snap.column("Name")).contains("Alpha"); assertThat(snap.columnValues("Properties").get(0)).containsExactly("Red", "Blue");`); point-in-time, no waiting, no remount safety; only `By.xpath` / `By.cssSelector`; hidden cell `""`, hidden item skipped; short row → `""` in `column`, `NoSuchElementException` from `cell`; only mounted rows (virtualized grids); when to prefer the lazy API (small tables, waiting assertions).
  - `### Cost`: append "To read a whole table, use `snapshot()` (see Bulk read)."
  - "Laziness and remount safety": add one sentence — "`snapshot()` is the exception: it returns plain data read once."

- [ ] **Step 2: Verify** `grep -nE "snapshot\(|Bulk read" docs/table-api.md` shows the method table rows, the section heading, the Cost pointer and the laziness sentence.

- [ ] **Step 3: Final check:** `./gradlew :integrations:selenide:check -x test --console=plain` → `BUILD SUCCESSFUL`; `BROWSER 'dev.quokkify.test.Table*'` and `UNIT 'dev.quokkify.elements.table.*'` → all PASS.

- [ ] **Step 4: Commit** — `docs(selenide): document Table.snapshot() bulk read`.
