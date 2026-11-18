package dev.quokkify.elements.table;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.WebElementCondition;
import org.openqa.selenium.WebElement;

/**
 * Checks a table root by reading a {@link TableSnapshot} in one browser call and running the caller's assertions on
 * it. Selenide re-evaluates the condition until it is accepted, so every attempt is a fresh, consistent read.
 */
final class SnapshotCondition extends WebElementCondition {

  private final String table;
  private final List<List<String>> locators;
  private final Consumer<TableSnapshot> assertions;

  SnapshotCondition(String table, List<List<String>> locators, Consumer<TableSnapshot> assertions) {
    super("table data satisfying the assertions");
    this.table = table;
    this.locators = locators;
    this.assertions = Objects.requireNonNull(assertions, "assertions");
  }

  @Override
  public CheckResult check(Driver driver, WebElement root) {
    Object[] args = Table.scriptArgs(root, locators);
    return evaluate(TableSnapshot.fromScript(driver.executeJavaScript(SnapshotScript.SOURCE, args), table),
        assertions);
  }

  /**
   * A failed assertion and a table that is not ready yet (header not displayed, row or cell not rendered, key column
   * incomplete) reject, so the condition is retried with the failure text as the actual value. Any other exception is
   * left to Selenide, which retries it like in any condition and reports it as the cause on timeout.
   */
  static CheckResult evaluate(TableSnapshot snapshot, Consumer<TableSnapshot> assertions) {
    try {
      assertions.accept(snapshot);
      return CheckResult.accepted();
    } catch (AssertionError e) {
      return rejected(Objects.toString(e.getMessage(), e.toString()));
    } catch (TableColumnException | TableKeyException | NoSuchElementException | IndexOutOfBoundsException e) {
      return rejected(e.toString());
    }
  }

  private static CheckResult rejected(String failure) {
    return CheckResult.rejected(failure, failure);
  }
}
