# integrations/selenide

Selenide-based UI automation framework with a fluent page-object model, typed step chains,
and a built-in verification layer with configurable polling timeout and Allure step reporting.

---

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/selenide):

```kotlin
dependencies {
    testImplementation("dev.quokkify:selenide:0.15.0")
}
```

The module brings `dev.quokkify:testng` (`AbstractSteps`) transitively; do not declare it separately.

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

## Tables

`dev.quokkify.elements.table.Table` adds three things plain Selenide lacks: columns by displayed header,
row lookup scoped to one column, and a failure on ambiguous headers. Everything else is ordinary Selenide.

```java
Table customers = Table.of($("#customers"), TableLayout.html());

customers.row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"));
customers.rows("Country", "Austria").shouldHave(size(2));
customers.column("Company").shouldHave(exactTexts("Alfreds Futterkiste", "Ernst Handel"));
```

For `@FindBy` page objects, wrap the helper in a small project component; see the page-object recipe in
[`docs/table-api.md`](../../docs/table-api.md#recipe-tables-in-page-objects-findby).

Layouts, behaviour, limits and the migration table from the removed table stack are in
[`docs/table-api.md`](../../docs/table-api.md).

## Dropdowns

Custom dropdowns (comboboxes, chip pickers) need no library type: opening, picking, chips and waiting are
plain Selenide. The recipes below are copied from
[`DropdownRecipeTest`](src/test/java/dev/quokkify/test/DropdownRecipeTest.java), which runs them against a
fixture. In a page object, apply them to the component root (`getSelf()` of a
`dev.quokkify.elements.base.Component`).

**Single select: type and pick.** Backed by `DropdownRecipeTest#testTypeAndSelectPartialFromClosed`.

```java
widget.find(byRole("combobox")).setValue("berry");
widget.find(byRole("listbox")).findAll(byRole("option")).findBy(partialText("berry")).click();

widget.find(".chip-row .item .item-label").shouldHave(exactText("Blueberry"));
```

**Chips: pick several, remove one by its label.** Match the label element, not the whole chip, so the
remove button's text never takes part in the match. Backed by `DropdownRecipeTest#testSelectSeveralAndRemoveOne`.

```java
widget.find(byRole("combobox")).click();
widget.find(byRole("listbox")).findAll(byRole("option")).findBy(exactText("Gamma")).click();
widget.find(byRole("combobox")).click();
widget.find(byRole("listbox")).findAll(byRole("option")).findBy(exactText("Delta")).click();
widget.findAll(".chip-row .item .item-label").findBy(exactText("Gamma")).closest(".item").find("button").click();

widget.findAll(".chip-row .item .item-label").shouldHave(exactTexts("Alpha", "Beta", "Delta"));
```

**Clear all chips when removal is asynchronous.** The `shouldHave(size(..))` after each click is what makes this
safe: without it, `first()` still points at the chip being removed. Backed by
`DropdownRecipeTest#testClearAllWithAsyncRemoval`.

```java
ElementsCollection chips = widget.findAll(".chip-row .item");

input.setValue("");
input.pressEscape();
for (int remaining = chips.size(); remaining > 0; remaining--) {
  chips.first().find("button").click();
  chips.shouldHave(size(remaining - 1));
}
chips.shouldBe(empty);
```

Close the popup first (`pressEscape()`): on many widgets an open popup covers the chip row and intercepts the
clicks.

**Listbox rendered in a portal.** Many widget libraries render the listbox into `<body>`, outside the widget,
and set `aria-controls` on the combobox only while it is open. Wait for the attribute, then locate the listbox
by id. Backed by `DropdownRecipeTest#testPortalSelectByClick`.

```java
SelenideElement listboxOf(SelenideElement combobox) {
  combobox.shouldHave(attribute("aria-controls"));
  return $(By.id(combobox.getAttribute("aria-controls")));
}

input.click();
listboxOf(input).findAll(byRole("option")).findBy(exactText("Pear")).click();
```

### Migration from `InputDropdown` / `MultiSelectInputDropdown`

Both classes were removed. Page objects that extended them should extend
`dev.quokkify.elements.base.Component` and use the recipes above on `getSelf()`. Below, `input` is the combobox,
`listbox` its listbox (`listboxOf(input)` for portals) and `labels` the chip label elements. Only the recipes
above are backed by tests; the table rows are not tested separately.

| Removed method                | Plain Selenide                                                                                                                                  |
| ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `selectExact(text)`           | `if (!listbox.isDisplayed()) input.click();` then `listbox.findAll(byRole("option")).findBy(exactText(text)).click();`                          |
| `selectPartial(text)`         | the same with `partialText(text)`                                                                                                               |
| `typeAndSelectExact(text)`    | `input.setValue(text);` then the option click with `exactText(text)`                                                                            |
| `typeAndSelectPartial(text)`  | `input.setValue(text);` then the option click with `partialText(text)`                                                                          |
| `typeAndPressEnter(text)`     | `input.setValue(text).pressEnter();`                                                                                                            |
| `selectAllExact(texts)`       | the `selectExact` lines for each text                                                                                                           |
| `selectAllPartial(texts)`     | the `selectPartial` lines for each text                                                                                                         |
| `selectedTexts()`             | `labels.texts()` for a snapshot, or `labels.shouldHave(exactTexts(..))` to wait                                                                 |
| `removeSelectedPartial(text)` | `int n = labels.size(); labels.findBy(partialTextCaseSensitive(text)).closest(".item").find("button").click(); labels.shouldHave(size(n - 1));` |
| `clearSelected()`             | `input.setValue(""); input.pressEscape();` then the clear-all loop above                                                                        |

Open the popup only when it is closed: on widgets where a click toggles the popup, clicking an open one closes it.
For portals, check `input` for `aria-expanded="true"` instead of `listbox.isDisplayed()`. Selenide text
conditions ignore case and collapse whitespace; the removed classes matched case-sensitively, so use the
`*CaseSensitive` variants where that matters. The removed classes also closed the popup after every operation:
when a widget stays open and the next step needs it closed, add `input.pressEscape()`.

---

## Run tests

```bash
./gradlew :integrations:selenide:test
```

---
