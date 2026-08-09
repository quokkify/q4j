# integrations/selenide

Selenide-based UI automation framework with a fluent page-object model, typed step chains,
and a built-in verification layer with configurable polling timeout and Allure step reporting.

---

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-selenide):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-selenide:0.2.2")
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

---

## Run tests

```bash
./gradlew :integrations:selenide:test
```
