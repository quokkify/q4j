package dev.quokkify.elements.table;

import com.codeborne.selenide.SelenideElement;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class TableSnapshotSoftAssertTest {

  /**
   * Regression guard: with {@code AssertionMode.SOFT} Selenide collects a failed {@code should} on a missing root and
   * returns without evaluating the condition, so {@code snapshot()} must not hand back {@code null}.
   */
  @Test
  public void snapshotFailsInsteadOfReturningNullWhenRootCheckDidNotRun() {
    Table table = Table.of(mock(SelenideElement.class), TableLayout.html());

    assertThatThrownBy(table::snapshot)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("was not read");
  }
}
