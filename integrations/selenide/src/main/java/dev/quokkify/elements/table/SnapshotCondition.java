package dev.quokkify.elements.table;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.WebElementCondition;
import org.openqa.selenium.WebElement;

/**
 * The one way a {@link Table} is read: a Selenide condition on the table root that reads a {@link TableSnapshot} in
 * one browser call with the root's own driver and judges it. Selenide re-locates the root and re-evaluates the
 * condition until it is accepted, so every attempt is a fresh, consistent read.
 */
final class SnapshotCondition extends WebElementCondition {

  private final String table;
  private final List<List<String>> locators;
  private final Function<TableSnapshot, CheckResult> judge;
  private TableSnapshot last;

  SnapshotCondition(String description, String table, List<List<String>> locators,
      Function<TableSnapshot, CheckResult> judge) {
    super(description);
    this.table = table;
    this.locators = locators;
    this.judge = Objects.requireNonNull(judge, "judge");
  }

  @Override
  public CheckResult check(Driver driver, WebElement root) {
    last = TableSnapshot.fromScript(driver.executeJavaScript(SnapshotScript.SOURCE, root, locators.get(0),
        locators.get(1), locators.get(2), locators.get(3)), table);
    return judge.apply(last);
  }

  /** The snapshot read by the last evaluation. */
  TableSnapshot last() {
    return last;
  }

  /**
   * A failed assertion and a table that is not ready yet (header not displayed or ambiguous, row or cell not rendered,
   * key cell missing or key repeated) reject, so the condition is retried with the failure text as the actual value.
   * Any other exception, typically a bug in the assertions, is not caught: Selenide does not retry it either, so it
   * fails the check at once as itself.
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
