# integrations/selenide

Selenide-based UI automation framework with a fluent page-object model, typed step chains,
and a built-in verification layer with configurable polling timeout and Allure step reporting.

---

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-selenide):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-selenide:0.5.1")
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

The neutral DOM model uses typed column keys mapped to the text displayed by the DOM; its enum
ordinal is never used as a column position. The additive `dev.quokkify.elements.table.model`
contract consists of `TableModel<C>`, `TableRow<C>`, and `TableCell<C>`. `DomTableLayout.CLASSIC`,
`DomTableLayout.FLEX`, and `DomTableLayout.HORIZONTAL` describe markup shape only.
`DisplayedHeaderResolver<C>` maps a typed key to its displayed header, and missing headers fail
with `TableColumnNotFoundException` rather than silently selecting a neighbouring column.

Rows and cells are allowed to be lazy: a concrete Selenide adapter may resolve the current DOM
element on each operation. `TableModel.rows()` represents rows currently available; adapters that
wait for asynchronous rows expose that policy through their existing timeout overloads. A required
row reports `TableRowNotFoundException` when the adapter's lookup policy expires. Optional row and
cell lookups use `Optional` and do not throw for missing data.
Required cells fail with `TableCellNotFoundException`; this is distinct from a missing table
header (`TableColumnNotFoundException`).

This model intentionally contains no sorting, filtering, pagination, selection, editing,
virtualization, or loading flags. Those capabilities must be separate components when added.

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
are defined: ARIA grids, virtualized rows, pagination/infinite scrolling, and
tree/master-detail tables. They are not implied by the current table abstractions.
