package dev.quokkify.elements.table;

import com.codeborne.selenide.impl.Cleanup;
import org.openqa.selenium.StaleElementReferenceException;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SnapshotScriptTest {

  /**
   * Regression guard: Selenium puts the executed script into the message of any error raised by that call, and
   * Selenide fails at once, without retry, on any error whose message contains an "invalid selector" marker. A stale
   * root (the table remounted between lookup and read) must stay retryable, so the script text must not carry one.
   */
  @Test
  public void errorQuotingTheScriptIsNotTakenForAnInvalidSelector() {
    StaleElementReferenceException stale = new StaleElementReferenceException(
        "stale element reference: stale element not found in the current frame\nCommand: [executeScript {script="
            + SnapshotScript.SOURCE + "}]");

    assertThat(Cleanup.of.isInvalidSelectorError(stale)).isFalse();
  }
}
