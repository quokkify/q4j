package dev.quokkify.test;

import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableColumnException;
import dev.quokkify.elements.table.TableLayout;
import dev.quokkify.elements.table.TableSnapshot;

import com.codeborne.selenide.ex.ElementNotFound;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TableSnapshotTest extends BaseTest {

  private static final String GRID_CELLS = ":scope > .cell:not([hidden])";

  @Test
  public void snapshotMatchesLazyApiOnHtmlLayout() {
    openClassicVariantsFixture();
    Table table = Table.of($("#classic"), TableLayout.html());
    table.rows().shouldHave(sizeGreaterThan(0));

    TableSnapshot snap = table.snapshot();

    assertThat(snap.headers()).containsExactly("Country", "Company").isEqualTo(table.headers().texts());
    assertThat(snap.column("Company")).isEqualTo(table.column("Company").texts());
  }

  @Test
  public void snapshotMatchesLazyApiOnAriaGrid() {
    openCustomGridsFixture();
    Table table = Table.of($("#aria-grid"), TableLayout.aria());
    table.rows().shouldHave(sizeGreaterThan(0));

    TableSnapshot snap = table.snapshot();

    assertThat(snap.headers()).containsExactly("Country", "Company");
    assertThat(snap.column("Company")).containsExactly("Alfreds");
  }

  @Test
  public void snapshotIgnoresNestedGridRows() {
    openCustomGridsFixture();
    Table table = grid("#custom-grid");
    table.rows().shouldHave(sizeGreaterThan(0));

    TableSnapshot snap = table.snapshot();

    assertThat(snap.rows()).hasSize(3);
    assertThat(snap.rows().get(2).cells()).hasSize(1);
    assertThat(snap.rows().get(1).cell("Company").text()).isEmpty();
    assertThat(snap.rows().get(0).cell("Company").text()).startsWith("Outer");
    for (int i = 0; i < 2; i++) {
      assertThat(snap.rows().get(i).cell("Country").text())
          .isEqualTo(table.row(i).cell("Country").text().trim());
    }
  }

  @Test
  public void snapshotDetectsAmbiguousHeader() {
    openEdgeCasesFixture();
    Table table = Table.of($("#repeated-table"), TableLayout.html());
    table.rows().shouldHave(sizeGreaterThan(0));

    TableSnapshot snap = table.snapshot();

    assertThatThrownBy(() -> snap.column("Company"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("ambiguous");
  }

  @Test
  public void snapshotOfMissingRootFailsWithElementNotFound() {
    openClassicVariantsFixture();
    Table table = Table.of($("#no-such-table"), TableLayout.html());

    assertThatThrownBy(table::snapshot).isInstanceOf(ElementNotFound.class);
  }

  private static Table grid(String selector) {
    return Table.of($(selector), TableLayout.of(
        By.cssSelector(":scope > .data-row"), By.cssSelector(GRID_CELLS),
        By.cssSelector(":scope > .header-row > .cell:not([hidden])")));
  }
}
