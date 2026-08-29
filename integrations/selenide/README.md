# integrations/selenide

Selenide-based UI automation framework with a fluent page-object model, typed step chains,
and a built-in verification layer with configurable polling timeout and Allure step reporting.

---

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-selenide):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-selenide:0.6.1")
}
```

---

## Architecture

The module uses a three-type-parameter CRTP pattern to keep step and verification chains
fully typed without casting:

```
PageSteps<S, V, P>  ──.verify()──►  Verification<S, V, P>  ──.backToSteps()──►  PageSteps
```

- `S` — concrete steps class
- `V` — concrete verification class
- `P` — concrete page class

---

## Implementing a page object

### 1. Page

```java
public class LoginPage extends Page {

    @FindBy(id = "username")
    private SelenideElement usernameInput;

    @FindBy(id = "password")
    private SelenideElement passwordInput;

    @FindBy(css = "button[type=submit]")
    private SelenideElement submitButton;

    public void fillUsername(String value) { usernameInput.setValue(value); }
    public void fillPassword(String value) { passwordInput.setValue(value); }
    public void submit()                   { submitButton.click(); }

    public boolean hasErrorMessage()       { return $(".error").exists(); }
}
```

### 2. Verification

```java
public class LoginVerification
    extends Verification<LoginSteps, LoginVerification, LoginPage> {

    public LoginVerification(LoginSteps steps, LoginPage page) {
        super(steps, page);
    }

    @Step("Error message is displayed")
    public LoginVerification errorMessageIsDisplayed() {
        Waiter.awaitCondition(
            () -> page.hasErrorMessage(),
            "Expected error message to appear",
            getTimeout(), getPollingInterval()
        );
        return this;
    }
}
```

### 3. Steps

```java
public class LoginSteps
    extends PageSteps<LoginSteps, LoginVerification, LoginPage> {

    public LoginSteps() {
        super(new LoginPage(), page -> new LoginVerification(null, page));
    }

    @Override
    protected LoginSteps self() { return this; }

    @Step("Login as {username}")
    public LoginSteps loginAs(String username, String password) {
        page.fillUsername(username);
        page.fillPassword(password);
        page.submit();
        return self();
    }
}
```

### 4. Usage in test

```java
loginSteps
    .loginAs("alice", "wrong-password")
    .verify()
    .withTimeout(Duration.ofSeconds(5))
    .errorMessageIsDisplayed()
    .backToSteps()
    .loginAs("alice", "correct-password");
```

---

## Timeout configuration

Default timeout is 10 seconds with 500 ms polling. Override per assertion block:

```java
steps.verify()
    .withTimeout(Duration.ofSeconds(15))
    .withPolling(Duration.ofMillis(200))
    .someCondition();
```

Use `getTimeout()` and `getPollingInterval()` inside `Verification` subclasses to pass the
configured values to `Waiter` calls.

## Table DOM model

The complete API record, compatibility boundary, weakness matrix, and upstream-evaluation prompt are
in [`docs/table-api.md`](../../docs/table-api.md) and
[`docs/selenide-upstream-evaluation-prompt.md`](../../docs/selenide-upstream-evaluation-prompt.md).

The neutral DOM model uses typed column keys mapped to the text displayed by the DOM; its enum
ordinal is never used as a column position. The additive `dev.quokkify.elements.table.model`
contract consists of `TableModel<C>`, `TableRow<C>`, and `TableCell<C>`. The public immutable
`TableDomAdapter` describes markup through relative Selenium locators and a header strategy.
`TableDomAdapters.classic()`, `flex()`, `horizontal()`, and `ariaGrid()` cover the built-in shapes;
`TableDomAdapters.of(...)` supports custom markup such as div grids. `DomTableLayout` and its
constructor remain as a compatibility bridge for legacy table components.
`DisplayedHeaderResolver<C>` maps a typed key to its displayed header, and missing headers fail
with `TableColumnNotFoundException` rather than silently selecting a neighbouring column.

These structural types are the extension contract. The current Selenide browser integration is one
backend adapter/plugin implementation; its `TableDomAdapter`, query, assertion, and action APIs
remain Selenide/Selenium-specific. A future separately published external Appium plugin/module may
depend on the structural contract and provide its own backend integration, but q4j core and this
Selenide module do not depend on or discover Appium. Appium has no implementation in this task, and
this PR introduces no runtime plugin loading.
The contracts currently ship in this `q4j-selenide` artifact alongside its Selenide dependency, so a
future plugin may pull Selenide transitively. Moving them to a neutral artifact/package is deferred
to a future major release to preserve the released 0.6.0 FQCNs.

Rows and cells are allowed to be lazy: a concrete Selenide adapter may resolve the current DOM
element on each operation. `TableModel.rows()` represents rows currently available; adapters that
wait for asynchronous rows expose that policy through their existing timeout overloads. A required
row reports `TableRowNotFoundException` when the adapter's lookup policy expires. Optional row and
cell lookups use `Optional` and do not throw for missing data.
Required cells fail with `TableCellNotFoundException`; this is distinct from a missing table
header (`TableColumnNotFoundException`).

Adapter locators determine which headers and cells are addressable, so hidden columns can be
excluded in both locators. Header-row, per-row-header, and headerless tables use
`TableHeaderRowLocator`, `RowHeaderCellLocator`, and `NoTableHeaders.instance()` respectively.
Repeated displayed headers remain ambiguous. Headerless typed lookup fails with
`TableColumnNotFoundException`. A mounted empty cell is present with empty text, while a missing
cell is `Optional.empty()`. Table, row, and cell locators are re-evaluated on each operation so
previously returned rows remain usable after a DOM remount.

Framework-specific markup stays outside the core API and is expressed through ordinary adapter
recipes. For example, Material-like and AG-Grid-like DOM can be described without adding a
dependency on either library:

```java
TableDomAdapter materialLike = TableDomAdapters.of(
    By.cssSelector(".mat-mdc-row"),
    By.cssSelector(":scope > .mat-mdc-cell"),
    new TableHeaderRowLocator(
        By.cssSelector(".mat-mdc-header-row"),
        By.cssSelector(":scope > .mat-mdc-header-cell")));

TableDomAdapter agGridLike = TableDomAdapters.of(
    By.cssSelector(".ag-center-cols-container > .ag-row"),
    By.cssSelector(":scope > .ag-cell"),
    new TableHeaderRowLocator(
        By.cssSelector(".ag-header-row"),
        By.cssSelector(":scope > .ag-header-cell")));
```

This model intentionally contains no sorting, filtering, pagination, selection, editing,
virtualization, or loading flags. Those capabilities must be separate components when added.

### Selenide table queries

Legacy table page objects expose an additive query layer without changing the neutral model:

```java
SelenideTableQuery<Header> query = table.query(header -> header.displayedName());

TableQueryRow<Header> row = query.requiredRow(
    RowConditions.all(
        RowConditions.exact(Header.COUNTRY, "Austria"),
        RowConditions.greaterThan(Header.EMPLOYEES, 10)),
    Duration.ofSeconds(2));

String company = row.requiredCell(Header.COMPANY).text();
List<? extends TypedTableCellRef<Header>> companies =
    query.column(Header.COMPANY).cells();
```

`row(int)`, `cell(int, int)`, and `column(int)` use zero-based indexes. Typed access uses the
caller-supplied `DisplayedHeaderResolver`; there is no implicit string-header overload. A table
whose key type is `String` must opt in with an explicit identity resolver.

`mountedRows()` includes hidden DOM rows, while `visibleRows()` filters them using Selenide's
displayed state. `findRow(...)` returns the first match, `findRows(...)` returns all matches in DOM
order, `requiredRow(...)` requires the first match, and `uniqueRow(...)` rejects both absence and
duplicate matches. `row(...)` is the concise equivalent of `requiredRow(...)`. Exact, substring,
regular-expression, numeric greater-than, and AND conditions are available from `RowConditions`.

Indexed and typed row/cell/column references contain only indexes or keys. They resolve the current
DOM when read, so they do not cache raw `WebElement` instances across a remount. Classic and flex
columns contain their matching cells down the mounted rows. A horizontal table's logical column
contains the single data cell beside that row's header.

### Selenide table assertions and actions

Query, row, cell, and column handles expose additive Selenide-native assertions. Table, row, and
column assertions run as one condition on the current table root, so headers, rows, and cells come
from the same DOM snapshot on every poll. Cell assertions delegate directly to Selenide's lazy
element condition. Handles retain only their root, adapter, row index, and column index or key;
they do not cache raw `WebElement` instances between operations.

```java
query.shouldHave(TableAssertions.rowCount(2))
    .shouldHave(TableAssertions.headers("Name", "Status", "Action"))
    .shouldHave(TableAssertions.matchingRow(
        RowAssertions.cell(Header.STATUS, Condition.exactText("Ready"))));

query.row(0).shouldHave(RowAssertions.values("Alpha", "Ready", "Run"));
query.column(Header.STATUS).shouldHave(ColumnAssertions.values("Ready", "Ready"));
query.row(0).requiredCell(Header.ACTION)
    .shouldBe(Condition.visible)
    .button().click();
```

Plain cell text and embedded controls have separate contracts. `input()`, `select()`, `checkbox()`,
`radio()`, `button()`, and `link()` return capability-specific lazy handles using standard HTML
semantics. Value editing is available only through `EditableTableControl`; `editable()` raises
`UnsupportedTableEditException` for a read-only cell. Custom key types remain independent of
`ConstantFormat`: callers always supply their own displayed-header resolver.

---

## Run tests

```bash
./gradlew :integrations:selenide:test
```

---

## Table support

The table elements support the DOM structures already represented by the module:

- classic HTML tables (`<table>`, header row followed by data rows), using enum ordinal
  columns with `Table`;
- classic HTML tables whose columns are resolved by displayed `<th>` text, using
  `DynamicTable` and `ConstantFormat`;
- flex tables represented by sibling `.flex-table-row` elements, where the first row is
  the header and is excluded from data-row lookups and column-value reads;
- horizontal tables represented by `<tr><th>header</th><td>value</td></tr>`, using
  `HorizontalTable` or `DynamicHorizontalTable`.

`getRow(...)` lookup methods and the neutral model's `requiredRow(..., Duration)` use one
Selenide-native condition loop, so rows or table containers may appear after page
initialization. Neutral-model `rows()` and row/cell handles re-resolve their locators when
read, which keeps them usable after DOM remounts. `isRowExist(...)` and the neutral model's
`row(...)` remain non-waiting status checks and return absence through `false`/`Optional.empty()`.

The following models are intentionally deferred until their DOM and behavior contracts
are defined: virtualized rows, pagination/infinite scrolling, and tree/master-detail
tables. ARIA grids are supported through `TableDomAdapters.ariaGrid()`.
