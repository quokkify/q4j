# Page objects

Write a Flutter screen as a q4j `Page`, its actions as `PageSteps` and its checks as `Verification`, exactly as for
a web page.

The three roles come from `integrations/selenide` (see its
[README](../../integrations/selenide/README.md#implementing-a-page-object)):

```text
PageSteps<S, V, P>  ──.verify()──►  Verification<S, V, P>  ──.backToSteps()──►  PageSteps
```

## What you write

The test reads as a sentence:

```java
@Test(description = "Login form enables its button only for valid input and greets the user")
public void logsIn() {
  new LoginScreenSteps(home().openLogin())
      .verify().loginButtonIsDisabled().backToSteps()
      .typeEmail(EMAIL)
      .verify().loginButtonIsDisabled().backToSteps()
      .typePassword("secret1")
      .verify().loginButtonIsEnabled().backToSteps()
      .submit()
      .verify().welcomes(EMAIL);
}
```

## Page: where the widgets are

```java
public class LoginScreen implements Page {

  public SelenideElement email() {
    return SelenideAppium.$(FlutterBy.key("login_email"));
  }

  public SelenideElement password() {
    return SelenideAppium.$(FlutterBy.key("login_password"));
  }

  // The key sits on a KeyedSubtree, which reports enabled=false: target the button under it.
  public SelenideElement loginButton() {
    return SelenideAppium.$(FlutterBy.descendant(FlutterBy.key("login_button"), FlutterBy.type("ElevatedButton")));
  }

  // The server reads text only from text widgets: target the Text under the key.
  public SelenideElement welcome() {
    return SelenideAppium.$(FlutterBy.descendant(FlutterBy.key("login_welcome"), FlutterBy.type("Text")));
  }

  public void type(SelenideElement field, String value) {
    field.click();
    field.setValue(value);
  }
}
```

The page returns lazy `SelenideElement`s and holds no state. Workarounds for the app's widget shape (wrappers,
labels inside buttons) live here and nowhere else.

## Steps: what the user does

```java
public class LoginScreenSteps extends PageSteps<LoginScreenSteps, LoginScreenVerification, LoginScreen> {

  public LoginScreenSteps(LoginScreen page) {
    super.verification = new LoginScreenVerification(this, page);
    super.page = page;
  }

  @Step("Type '{email}' as the email")
  public LoginScreenSteps typeEmail(String email) {
    page.type(page.email(), email);
    return this;
  }

  @Step("Type the password")
  public LoginScreenSteps typePassword(String password) {
    page.type(page.password(), password);
    return this;
  }

  @Step("Submit the login form")
  public LoginScreenSteps submit() {
    page.loginButton().click();
    return this;
  }
}
```

## Verification: what the user sees

```java
public class LoginScreenVerification extends Verification<LoginScreenSteps, LoginScreenVerification, LoginScreen> {

  public LoginScreenVerification(LoginScreenSteps steps, LoginScreen page) {
    super(steps, page);
  }

  @Step("Verify the login button is disabled")
  public LoginScreenVerification loginButtonIsDisabled() {
    page.loginButton().shouldBe(Condition.visible, getTimeout()).shouldBe(Condition.disabled, getTimeout());
    return this;
  }

  @Step("Verify the login button is enabled")
  public LoginScreenVerification loginButtonIsEnabled() {
    page.loginButton().shouldBe(Condition.enabled, getTimeout());
    return this;
  }

  @Step("Verify the welcome text greets '{email}'")
  public LoginScreenVerification welcomes(String email) {
    page.welcome().shouldHave(Condition.exactText("Welcome, " + email), getTimeout());
    return this;
  }
}
```

`getTimeout()` is 10 s unless the test calls `.verify().withTimeout(...)`.

Each `@Step` becomes an Allure step, so a failed find inside `submit()` attaches its
[diagnostics](reports-and-diagnostics.md) to "Submit the login form".

## One page, two drivers

To run the same scenario through the Flutter driver and natively, route every locator through one object that
knows the mode. The device tests' `ModeLocators` does this:

```java
public final class ModeLocators {

  private final MobileMode mode;

  public By id(String id) {
    return mode.isFlutter() ? FlutterBy.key(id) : AppiumBy.id(id);
  }

  public By textOf(String id) {
    return mode.isFlutter() ? FlutterBy.descendant(FlutterBy.key(id), FlutterBy.type("Text")) : id(id);
  }

  public WebElementCondition shows(String text) {
    return mode.isFlutter() ? Condition.exactText(text) : Condition.attribute("content-desc", text);
  }
}
```

The page takes a `ModeLocators` in its constructor and calls `locators.id("login_email")`. Natively Flutter
exposes a widget's text as `content-desc` and leaves `text` empty, which is why `shows` differs by mode.
[Flutter vs native](flutter-vs-native.md) lists the other differences.
