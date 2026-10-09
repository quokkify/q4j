# Thin Selenide Table Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the q4j table stack in `integrations/selenide` with five small types (`Table`, `TableRow`, `TableLayout`, `HorizontalTable`, `TableColumnException`) on top of plain Selenide.

**Architecture:** A `Table` holds a lazy root `SelenideElement` and a `TableLayout` (three relative `By`s). Everything it returns is a native lazy `SelenideElement` / `ElementsCollection`. Column-scoped row lookup is a package-private `WebElementCondition` that resolves the header index on every evaluation, so it waits, survives remounts and header reordering through Selenide's own `should*` loop.

**Tech Stack:** Java 21, Selenide 7.18.2, TestNG, AssertJ, Gradle, local `web` profile (nginx + Selenium Grid).

**Spec:** `docs/superpowers/specs/2026-10-09-thin-table-design.md`

## Global Constraints

- Package of new production types: `dev.quokkify.elements.table` in `integrations/selenide/src/main/java`.
- Public types: exactly `Table`, `TableRow`, `TableLayout`, `HorizontalTable`, `TableColumnException`. Helpers are package-private.
- Return only `SelenideElement`, `ElementsCollection`, `TableRow`, `String`, `List<String>`; no custom assertion, condition or control types in the public API.
- Header match: exact, case-sensitive, after `String.trim()` of the displayed header text.
- No changes outside `integrations/selenide`, `docs/`, and fixtures are not modified. `common-utils/core` (`NumberFormatter`, `LocalDateUtils`) stays.
- Code style: 2-space indent, imports `java.*` / `dev.quokkify.*` / third-party / static, no star imports; checkstyle `tools/checkstyle/checkstyle.xml` must pass.
- Commits: Conventional Commits, scope `selenide`; the PR title is `feat(selenide)!: replace table stack with thin Table helper` with a `BREAKING CHANGE:` footer.
- Do not run `git worktree`, do not push, do not open PRs except in Task 7.

## Commands

- `COMPILE` = `./gradlew :integrations:selenide:compileTestJava :integrations:selenide:checkstyleMain :integrations:selenide:checkstyleTest --console=plain` → `BUILD SUCCESSFUL`.
- `UNIT <pattern>` = `./gradlew :integrations:selenide:test --tests '<pattern>' --console=plain` (no browser).
- `BROWSER <pattern...>` = `CI= BASE_URL=http://host.docker.internal:80 NGINX_BASE_URL=http://host.docker.internal:80 BROWSER_REMOTE_URL=http://localhost:4444/wd/hub ./gradlew :integrations:selenide:test --tests '<pattern>' --console=plain`. Requires `env -u CI ./tools/environment/scripts/infra/run_app.sh web` to be up (`curl -s http://localhost:4444/wd/hub/status` shows `"ready": true`).

## Review Focus

1. **Root not mounted yet** (`/table/late-mounting-table.html`, `#late-customers` appears after 1.5 s): header list is empty → the row condition must report "no match" and keep waiting, not throw `TableColumnException`. Test in Task 2.
2. **Row shorter than the resolved column** (`#custom-grid`, the `Germany` row has 1 cell): must not match and must not throw `IndexOutOfBoundsException`. Test in Task 2.
3. **Hidden header cell** (`#custom-grid` has hidden `Internal ID` between `Country` and `Company`): header and cell indexes must stay aligned, so `Company` resolves to cell index 2. Test in Task 2.
4. **Header text with surrounding whitespace / line breaks**: `" Company \n"` must resolve as `Company`. Unit test in Task 1.
5. **Duplicate header used inside a row lookup** (`#repeated-table`): `row("Company", …)` must fail with `TableColumnException` (ambiguous), not silently use the first column. Test in Task 2.

---

### Task 1: `TableLayout`, `TableColumnException`, header resolution

**Files:**

- Create: `integrations/selenide/src/main/java/dev/quokkify/elements/table/TableLayout.java`
- Create: `integrations/selenide/src/main/java/dev/quokkify/elements/table/TableColumnException.java`
- Create: `integrations/selenide/src/main/java/dev/quokkify/elements/table/ColumnResolver.java` (package-private)
- Test: `integrations/selenide/src/test/java/dev/quokkify/elements/table/TableLayoutTest.java`
- Test: `integrations/selenide/src/test/java/dev/quokkify/elements/table/ColumnResolverTest.java`

Note: `integrations/selenide/src/test/java/dev/quokkify/elements/table/model/` already exists; the new tests go one level up, in `dev.quokkify.elements.table`.

**Interfaces:**

- Produces:
  - `public record TableLayout(By rows, By cells, By headers)` with `static TableLayout html()`, `static TableLayout aria()`, `static TableLayout of(By rows, By cells, By headers)`; compact constructor rejects nulls via `Objects.requireNonNull(x, "rows"|"cells"|"headers")`; package-private `static Optional<String> xpath(By by)` returns the expression for `By.xpath`, empty otherwise.
  - `public class TableColumnException extends RuntimeException` with `TableColumnException(String header, String reason, String table, List<String> displayedHeaders)`.
  - `final class ColumnResolver` with `static int indexOf(List<String> displayedHeaders, String header, String table)`; throws `TableColumnException` with reason `"not found"` or `"ambiguous"`.

- [ ] **Step 1: Write the failing tests**

`TableLayoutTest` (TestNG, AssertJ):

```java
@Test public void htmlLayoutUsesDirectChildXpath() {
  assertThat(TableLayout.html()).isEqualTo(new TableLayout(
      By.xpath("./tbody/tr[td]"), By.xpath("./*[self::td or self::th]"), By.xpath("./thead/tr/th")));
}
@Test public void extractsXpathExpression() {
  assertThat(TableLayout.xpath(By.xpath("./tbody/tr"))).contains("./tbody/tr");
  assertThat(TableLayout.xpath(By.cssSelector("tr"))).isEmpty();
}
@Test public void ariaLayoutUsesExplicitRoles() {
  assertThat(TableLayout.aria()).isEqualTo(new TableLayout(
      By.xpath(".//*[@role='row'][*[@role='cell' or @role='gridcell']]"),
      By.xpath("./*[@role='cell' or @role='gridcell' or @role='rowheader']"),
      By.xpath(".//*[@role='columnheader']")));
}
@Test public void rejectsNullLocators() {
  assertThatThrownBy(() -> TableLayout.of(null, By.id("c"), By.id("h")))
      .isInstanceOf(NullPointerException.class).hasMessage("rows");
}
```

`ColumnResolverTest`:

```java
@Test public void resolvesTrimmedExactHeader() {
  assertThat(ColumnResolver.indexOf(List.of("Country", " Company \n"), "Company", "#t")).isEqualTo(1);
}
@Test public void isCaseSensitive() {
  assertThatThrownBy(() -> ColumnResolver.indexOf(List.of("company"), "Company", "#t"))
      .isInstanceOf(TableColumnException.class);
}
@Test public void reportsMissingHeaderWithDisplayedHeaders() {
  assertThatThrownBy(() -> ColumnResolver.indexOf(List.of("Country", "Company"), "Region", "#t"))
      .isInstanceOf(TableColumnException.class)
      .hasMessage("Column \"Region\" not found in #t; displayed headers: [Country, Company]");
}
@Test public void reportsAmbiguousHeader() {
  assertThatThrownBy(() -> ColumnResolver.indexOf(List.of("Country", "Company", "Company"), "Company", "#t"))
      .isInstanceOf(TableColumnException.class)
      .hasMessage("Column \"Company\" ambiguous in #t; displayed headers: [Country, Company, Company]");
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `UNIT 'dev.quokkify.elements.table.TableLayoutTest'` and `UNIT 'dev.quokkify.elements.table.ColumnResolverTest'`
Expected: compilation FAIL (`cannot find symbol TableLayout` / `ColumnResolver`).

- [ ] **Step 3: Implement** the three types with the signatures above. Message format is fixed by the tests: `Column "<header>" <reason> in <table>; displayed headers: <list>`; the list is the untrimmed `List.toString()` of the trimmed headers.

- [ ] **Step 4: Run tests to verify they pass**

Run: the two `UNIT` commands, then `COMPILE`. Expected: PASS, `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add integrations/selenide/src/main/java/dev/quokkify/elements/table/{TableLayout,TableColumnException,ColumnResolver}.java \
        integrations/selenide/src/test/java/dev/quokkify/elements/table/{TableLayoutTest,ColumnResolverTest}.java
git commit -m "feat(selenide): add TableLayout and column resolution"
```

---

### Task 2: `Table` and `TableRow`

**Files:**

- Create: `integrations/selenide/src/main/java/dev/quokkify/elements/table/Table.java`
- Create: `integrations/selenide/src/main/java/dev/quokkify/elements/table/TableRow.java`
- Create: `integrations/selenide/src/main/java/dev/quokkify/elements/table/ColumnValueCondition.java` (package-private)
- Test: `integrations/selenide/src/test/java/dev/quokkify/test/TableTest.java` (extends `BaseTest`, browser)

**Interfaces:**

- Consumes: `TableLayout`, `ColumnResolver.indexOf`, `TableColumnException` (Task 1).
- Produces:
  - `public final class Table`: `static Table of(SelenideElement root, TableLayout layout)`; `SelenideElement root()`; `ElementsCollection headers()` = `root.$$(layout.headers())`; `ElementsCollection rows()` = `root.$$(layout.rows())`; `TableRow row(int index)`; `TableRow row(String column, String value)`; `ElementsCollection rows(String column, String value)`; `ElementsCollection column(String header)`.
  - `public final class TableRow`: `SelenideElement self()`; `ElementsCollection cells()` = `self.$$(layout.cells())`; `SelenideElement cell(int index)`; `SelenideElement cell(String header)`.
  - `final class ColumnValueCondition extends WebElementCondition` — constructor `(Table table, String column, String value)`, `toString()` / name = `<column> = "<value>"` (e.g. `Company = "Ernst Handel"`).

Behaviour the implementer must follow (from the spec):

- `row(column, value)` = `new TableRow(rows().findBy(new ColumnValueCondition(this, column, value)), layout)`; `rows(column, value)` = `rows().filterBy(...)` with the same condition.
- `ColumnValueCondition.check(Driver, WebElement row)`: read `table.headers().texts()`; **if empty → `CheckResult.rejected`** (root or headers not mounted yet, keep waiting — Review Focus 1); otherwise `ColumnResolver.indexOf(...)` (throws on missing/ambiguous — Review Focus 5); read `row.findElements(layout.cells())`; if `index >= cells.size()` → rejected (Review Focus 2); else accepted iff `cells.get(index).getText().trim().equals(value)`.
- `TableRow.cell(header)`: `headers().shouldHave(sizeGreaterThan(0))`, then `i = ColumnResolver.indexOf(headers().texts(), header, root.toString())`, return `cells().get(i)`.
- `Table.column(header)`: same index resolution, then `root.$$(By.xpath(rowsXpath + "/" + cellStep + "[" + (i + 1) + "]"))`, where `rowsXpath` is the layout's rows XPath and `cellStep` is the cells XPath without its leading `./` (e.g. `*[self::td or self::th]`). This is valid XPath 1.0 because the cells locator is a single child step with a predicate. Supported only when both `rows` and `cells` are `By.xpath` and the cells XPath starts with `./` and contains no `|`; otherwise throw `UnsupportedOperationException("column() needs XPath row and single-step cell locators; use rows() and TableRow.cell()")`. Read the XPath string from `By.toString()` (`"By.xpath: <expr>"`) via a package-private helper in `TableLayout`: `Optional<String> xpath(By by)`. Document the limitation in Javadoc.

- [ ] **Step 1: Write the failing tests** in `TableTest` (all `@Test`, fixtures opened with `BaseTest` helpers or `Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html")`; `TIMEOUT = Duration.ofSeconds(5)`):

```java
public void findsDelayedRowByColumnValue()            // delayed-table #customers, html()
  customers.row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
public void matchesValueOnlyInGivenColumn()           // queries #query-classic, html()
  t.rows("Company", "Austria").shouldHave(size(0));
  t.rows("Country", "Austria").shouldHave(size(2));
public void returnsAllMatchingRows()                  // queries #query-classic
  t.rows("Country", "Austria").shouldHave(size(2));
  t.row("Country", "Austria").cell("Company").shouldHave(exactText("Alfreds"));
public void readsColumnByHeader()                     // delayed-table #customers
  customers.column("Company").shouldHave(exactTexts("Alfreds Futterkiste", "Ernst Handel"), TIMEOUT);
public void missingHeaderThrowsTableColumnException() // queries #query-classic
  assertThatThrownBy(() -> t.row(0).cell("Region")).isInstanceOf(TableColumnException.class)
      .hasMessageContaining("Region").hasMessageContaining("[Country, Company, Employees]");
public void duplicateHeaderInRowLookupThrows()        // edge-cases #repeated-table  (Review Focus 5)
  assertThatThrownBy(() -> t.row("Company", "x").self().should(exist, Duration.ofMillis(500)))
      .isInstanceOf(TableColumnException.class).hasMessageContaining("ambiguous");
public void missingRowFailsWithElementNotFound()      // delayed-table #customers
  assertThatThrownBy(() -> customers.row("Company", "Missing Company").self().should(exist, Duration.ofMillis(600)))
      .isInstanceOf(ElementNotFound.class).hasMessageContaining("Company = \"Missing Company\"");
public void capturedRowSurvivesRemount()              // delayed-table #customers, window.reloadClassicTable()
  TableRow row = customers.row("Company", "Ernst Handel");
  row.cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
  Selenide.executeJavaScript("window.reloadClassicTable()");
  row.cell("Country").shouldHave(exactText("Austria reloaded"), TIMEOUT);
public void rowLookupSurvivesHeaderReorder()          // queries, window.remountQueryClassicWithReorderedHeaders()
  TableRow row = t.row("Company", "Berglunds");
  row.self().shouldHave(text("Germany"));
  Selenide.executeJavaScript("window.remountQueryClassicWithReorderedHeaders()");
  row.cell("Country").shouldHave(exactText("Germany"));
public void htmlLayoutIgnoresNestedTableRows()        // edge-cases #nested-classic
  t.rows().shouldHave(size(<outer row count from fixture>));
public void waitsForLateMountedRoot()                 // /table/late-mounting-table.html #late-customers  (Review Focus 1)
  Table late = Table.of($("#late-customers"), TableLayout.of(
      By.xpath("./tbody/tr[td]"), By.xpath("./td"), By.xpath("./tbody/tr[th]/th")));
  late.row("Company", "Ernst Handel").self().shouldBe(visible, TIMEOUT);
public void readsAriaGrid()                           // custom-grids #aria-grid, aria()
  t.headers().shouldHave(exactTexts("Country", "Company"));
  t.row("Country", "Austria").cell("Company").shouldHave(exactText("Alfreds"));
public void readsCustomDivGridWithHiddenHeaderCell()  // custom-grids #custom-grid  (Review Focus 2, 3)
  Table grid = Table.of($("#custom-grid"), TableLayout.of(By.cssSelector(":scope > .data-row"),
      By.cssSelector(":scope > .cell"), By.cssSelector(":scope > .header-row > .cell")));
  grid.rows().shouldHave(size(3));
  grid.row("Country", "Austria").cell("Company").shouldHave(text("Outer"));
  grid.rows("Company", "anything").shouldHave(size(0));   // Germany row has 1 cell: no IOOBE
public void readsFlexTable()                          // delayed-table #flex-customers
  Table flex = Table.of($("#flex-customers"), TableLayout.of(
      By.cssSelector(":scope > .flex-table-row:not(:first-child)"), By.cssSelector(":scope > div"),
      By.cssSelector(":scope > .flex-table-row:first-child > div")));
  flex.row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
public void columnRequiresXpathLayout()               // delayed-table #flex-customers
  assertThatThrownBy(() -> flex.column("Company")).isInstanceOf(UnsupportedOperationException.class);
```

Fill `<outer row count from fixture>` by reading `tools/environment/assets/nginx/html/table-model-contract/edge-cases.html` `#nested-classic`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `BROWSER 'dev.quokkify.test.TableTest'`. Expected: compilation FAIL (`cannot find symbol Table`).

- [ ] **Step 3: Implement** `Table`, `TableRow`, `ColumnValueCondition` per the Interfaces block and behaviour list.

- [ ] **Step 4: Run tests to verify they pass**

Run: `COMPILE`, then `BROWSER 'dev.quokkify.test.TableTest'`. Expected: all `TableTest` tests PASS.

- [ ] **Step 5: Commit**

```bash
git add integrations/selenide/src/main/java/dev/quokkify/elements/table/{Table,TableRow,ColumnValueCondition}.java \
        integrations/selenide/src/test/java/dev/quokkify/test/TableTest.java
git commit -m "feat(selenide): add thin Table and TableRow helpers"
```

---

### Task 3: `HorizontalTable`

**Files:**

- Create: `integrations/selenide/src/main/java/dev/quokkify/elements/table/HorizontalTable.java`
- Test: `integrations/selenide/src/test/java/dev/quokkify/test/HorizontalTableTest.java` (extends `BaseTest`)

**Interfaces:**

- Produces: `public final class HorizontalTable`: `static HorizontalTable of(SelenideElement root)`; `ElementsCollection headers()` = `root.$$x(".//tr/th")`; `SelenideElement value(String header)` = `root.$$x(".//tr").findBy(<th has exact trimmed text header>).$x("./td")`. Condition: `Condition.match("header = \"" + header + "\"", tr -> tr.findElements(By.xpath("./th")).stream().anyMatch(th -> th.getText().trim().equals(header)))`.

- [ ] **Step 1: Write the failing tests** (delayed-table `#horizontal-customers`, `TIMEOUT = 5s`):

```java
public void readsValueByHeader()        h.value("Name").shouldHave(exactText("Bill Gates"));
public void waitsForDelayedHeader()     h.value("Telephone 2").shouldHave(exactText("555 77 855"), TIMEOUT);
public void survivesRemount()           // after Telephone 2 appears: executeJavaScript("window.reloadHorizontalTable()")
                                        h.value("Telephone 2").shouldHave(exactText("555 77 856"), TIMEOUT);
public void missingHeaderFails()        assertThatThrownBy(() -> h.value("Missing Header").should(exist, Duration.ofMillis(600)))
                                          .isInstanceOf(ElementNotFound.class).hasMessageContaining("Missing Header");
public void doesNotMatchHeaderPrefix()  h.value("Telephone").should(not(exist));
```

- [ ] **Step 2: Run** `BROWSER 'dev.quokkify.test.HorizontalTableTest'` → compilation FAIL.
- [ ] **Step 3: Implement** `HorizontalTable`.
- [ ] **Step 4: Run** `COMPILE`, `BROWSER 'dev.quokkify.test.HorizontalTableTest'` → PASS.
- [ ] **Step 5: Commit** `git commit -m "feat(selenide): add HorizontalTable helper"` (the two files).

---

### Task 4: Migrate existing table tests to the new helpers

**Files (modify):** in `integrations/selenide/src/test/java/dev/quokkify/test/`: `UiTableTest`, `TableRowWaitTest`, `TableModelContractTest`, `TableQueryContractTest`, `TableAssertionsActionsContractTest`, `UiHorizontalTableTest`, `ReproHorizontalAsyncTest`.

**Interfaces:**

- Consumes: `Table`, `TableRow`, `TableLayout`, `HorizontalTable`, `TableColumnException` (Tasks 1–3).

Rules:

- Remove every private `columnIndex(...)`, `classicHeaders/Rows/Cells`, `gridHeaders/Rows/Cells`, `rows/cells` helper and the column-scoped `Condition.match` helper `cell(int, String, Predicate)`; replace call sites with `Table` / `TableRow` / `HorizontalTable`.
- Keep test method names, `@Test` descriptions, `@TmsLink`, data providers, `@SingleThread`, and the three methods referenced by the `tableModelContractStability` task in `integrations/selenide/build.gradle`.
- Where a test asserts a DOM fact on purpose (e.g. duplicate headers via `exactTexts`), switch to the helper's behaviour when it exists: duplicate header → `assertThatThrownBy(...).isInstanceOf(TableColumnException.class)`; missing header → same.
- Plain-Selenide code that is already a one-liner (e.g. `$$("tbody tr").findBy(text(...))` used for a substring check) may stay.

- [ ] **Step 1: Migrate the seven classes.**
- [ ] **Step 2: Verify no hand-written helpers remain**

Run: `grep -nE "int columnIndex\(|classicCells|gridCells|Condition\.match" integrations/selenide/src/test/java/dev/quokkify/test/*.java`
Expected: no output.

- [ ] **Step 3: Run** `COMPILE`, then `BROWSER` with the seven classes plus `TableTest` and `HorizontalTableTest` (one `--tests` per class). Expected: all PASS; total test count = 73 + Task 2 + Task 3 test counts.
- [ ] **Step 4: Commit** `git commit -m "test(selenide): use Table helpers in table tests"`.

---

### Task 5: Remove the old table stack and dead test support

**Files (delete):**

- `integrations/selenide/src/main/java/dev/quokkify/elements/table/classic/` (whole directory)
- `integrations/selenide/src/main/java/dev/quokkify/elements/table/horizontal/` (whole directory)
- `integrations/selenide/src/main/java/dev/quokkify/elements/table/model/` (whole directory)
- `integrations/selenide/src/main/java/dev/quokkify/elements/base/BaseTable.java`
- `integrations/selenide/src/main/java/dev/quokkify/ex/TableRowException.java`
- `integrations/selenide/src/test/java/dev/quokkify/test/RowConditionsContractTest.java`
- `integrations/selenide/src/test/java/dev/quokkify/elements/table/model/SelenideRowConditionSnapshotTest.java`
- Test support with no remaining test users after PR #726: `page/local/DelayedTablePage.java`, `page/local/LateMountingTablePage.java`, `page/w3school/{BaseTablePage,HtmlTablesPage,HtmlHorizontalTablePage}.java`, `service/steps/w3schools/*.java`, `service/verifications/w3schools/*.java` (all under `integrations/selenide/src/test/java/dev/quokkify/`).

Keep: `elements/base/Component.java` (used by dropdowns), `common-utils/*`.

- [ ] **Step 1: Confirm the test-support files are unused**

Run: `grep -rlE "DelayedTablePage|LateMountingTablePage|HtmlTablesPage|HtmlHorizontalTablePage|W3SchoolsNavigationSteps|BaseTablePage" integrations/selenide/src/test/java | grep -vE "/page/(local|w3school)/|/service/(steps|verifications)/w3schools/"`
Expected: no output (`TableRowWaitTest` only has a method named `openDelayedTablePage`, which does not match `DelayedTablePage` as a type — if grep reports it, verify it is the method name only).

- [ ] **Step 2: Delete** the listed files with `git rm -r`.
- [ ] **Step 3: Verify nothing references removed packages**

Run: `grep -rnE "dev\.quokkify\.elements\.table\.(classic|horizontal|model)|elements\.base\.BaseTable|ex\.TableRowException" --include='*.java' --include='*.gradle' . | grep -v /build/`
Expected: no output.

- [ ] **Step 4: Run** `./gradlew :integrations:selenide:check -x test --console=plain` (compile, checkstyle, other static checks) → `BUILD SUCCESSFUL`; then the full `BROWSER` run from Task 4 Step 3 → all PASS.
- [ ] **Step 5: Commit**

```bash
git commit -m "feat(selenide)!: remove legacy table stack

BREAKING CHANGE: dev.quokkify.elements.table.classic, .horizontal and .model,
BaseTable and TableRowException are removed. Use Table, TableRow, TableLayout,
HorizontalTable and TableColumnException from dev.quokkify.elements.table."
```

---

### Task 6: Documentation

**Files:**

- Modify: `docs/table-api.md` (rewrite around the five types; examples from the spec's Public API section; documented limits: hidden rows count in indexes, `aria()` matches nested grids, `column()` needs XPath layouts, `cell(header)` resolves the index at call time, `html()` needs `<thead>`).
- Modify: `integrations/selenide/README.md` — replace the "Table DOM model" section with a short usage example and a link to `docs/table-api.md`.
- Modify: `integrations/selenide/docs/AUDIT.md`, `docs/selenide-upstream-evaluation-prompt.md` — replace references to removed types.
- Modify: `docs/superpowers/specs/2026-10-09-thin-table-design.md` — in "Tests", replace "Migrate page objects, steps and verifications" with "Delete them: no tests use them after PR #726"; in `Table` API add the `column()` XPath limitation; in the `TableLayout` factories table change the `html()` cells locator to `./*[self::td or self::th]`.

- [ ] **Step 1: Edit the files.**
- [ ] **Step 2: Verify no stale names**

Run: `grep -rnE "SelenideTableQuery|TableDomAdapter|DynamicTable|FlexTable|SelenideDataTable|RowConditions|TableModel<" docs integrations/selenide/README.md integrations/selenide/docs`
Expected: matches only inside the spec's "Removed" table.

- [ ] **Step 3: Commit** `git commit -m "docs(selenide): document thin Table helper"`.

---

### Task 7: Final verification and PR

- [ ] **Step 1:** `./gradlew :integrations:selenide:check -x test --console=plain` → `BUILD SUCCESSFUL`.
- [ ] **Step 2:** Full browser run of all classes in `dev.quokkify.test` that touch tables plus `TableTest`, `HorizontalTableTest`, and `UNIT 'dev.quokkify.elements.table.*'` → 0 failures. Record the test count.
- [ ] **Step 3:** Line count check: `wc -l integrations/selenide/src/main/java/dev/quokkify/elements/table/*.java` → about 250 lines total (report the number; above 400 needs a note in the PR).
- [ ] **Step 4:** `git push -u origin feat/selenide-thin-table`; open the PR in `quokkify/q4j` with base `test/selenide-table-recipes` (stacked on #726), title `feat(selenide)!: replace table stack with thin Table helper`, body: summary, removed → replacement table from the spec, `BREAKING CHANGE:` paragraph, verification commands and counts.
