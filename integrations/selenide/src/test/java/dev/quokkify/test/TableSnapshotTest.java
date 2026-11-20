package dev.quokkify.test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.quokkify.annotation.Sequential;
import dev.quokkify.elements.table.SnapshotCell;
import dev.quokkify.elements.table.SnapshotRow;
import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableColumnException;
import dev.quokkify.elements.table.TableLayout;
import dev.quokkify.elements.table.TableSnapshot;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.ex.ElementNotFound;
import org.openqa.selenium.By;
import org.openqa.selenium.support.events.WebDriverListener;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TableSnapshotTest extends BaseTest {

  private static final String GRID_CELLS = ":scope > .cell:not([hidden])";
  private static final int LARGE_ROWS = 1200;

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
  public void snapshotReadsAriaGrid() {
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

  @Test
  public void readsChipsWithItemLocator() {
    openSnapshotFixture();
    Table table = chipGrid();

    TableSnapshot snap = table.snapshot(By.cssSelector(".chip"));

    assertThat(snap.column("Name")).containsExactly("Alpha", "Beta", "Gamma");
    assertThat(snap.columnItems("Properties"))
        .containsExactly(List.of("Red", "Blue"), List.of("Green"), List.of("Violet"));
    assertThat(snap.rows().get(2).cell("Properties").text()).isEqualTo("Violet")
        .isEqualTo(table.row(2).cell("Properties").text());
  }

  @Test
  public void hiddenCellReadsEmptyInBothApis() {
    openSnapshotFixture();
    Table table = chipGrid();

    TableSnapshot snap = table.snapshot();

    assertThat(snap.rows().get(0).cell("Secret").text()).isEmpty();
    assertThat(table.row(0).cell("Secret").text()).isEmpty();
    assertThat(snap.rows().get(1).cell("Secret").text()).isEqualTo("s2");
    assertThat(snap.rows().get(1).cell("Properties").text()).isEqualTo("Green")
        .isEqualTo(table.row(1).cell("Properties").text().trim());
    assertThat(snap.rows().get(2).cell("Secret").text()).isEmpty();
    assertThat(table.row(2).cell("Secret").text()).isEmpty();
  }

  @Test
  public void snapshotWithoutItemsHasNoValues() {
    openSnapshotFixture();

    TableSnapshot snap = chipGrid().snapshot();

    assertThat(snap.rows()).flatExtracting(SnapshotRow::cells)
        .allSatisfy(cell -> assertThat(((SnapshotCell) cell).items()).isEmpty());
  }

  @Test
  @Sequential
  public void snapshotMakesNoPerRowDriverCalls() {
    // Selenide unwraps the decorated driver before executeScript, so the script call itself is invisible here;
    // every element-level call a per-row read would need (findElements, getText, isDisplayed) is visible.
    List<String> calls = new CopyOnWriteArrayList<>();
    Thread testThread = Thread.currentThread();
    WebDriverListener recorder = new WebDriverListener() {
      @Override
      public void beforeAnyCall(Object target, Method method, Object[] args) {
        if (Thread.currentThread() == testThread) {
          calls.add(method.getName());
        }
      }
    };
    WebDriverRunner.addListener(recorder);
    try {
      openSnapshotFixture();
      Table small = chipGrid();
      Table large = largeGrid();

      calls.clear();
      small.snapshot();
      List<String> smallCalls = List.copyOf(calls);
      calls.clear();
      TableSnapshot snap = large.snapshot();
      List<String> largeCalls = List.copyOf(calls);

      assertThat(snap.rows()).hasSize(LARGE_ROWS);
      assertThat(smallCalls).as("driver calls recorded during a snapshot").contains("findElement");
      assertThat(largeCalls).isEqualTo(smallCalls).doesNotContain("findElements", "getText", "getDomProperty",
          "getAttribute");
    } finally {
      WebDriverRunner.removeListener(recorder);
    }
  }

  @Test
  @Sequential
  public void largeTableReadsFast() {
    openSnapshotFixture();
    Table table = largeGrid();

    long start = System.nanoTime();
    TableSnapshot snap = table.snapshot();
    Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

    assertThat(elapsed).isLessThan(Duration.ofSeconds(10));
    assertThat(snap.column("Value").get(LARGE_ROWS - 1)).isEqualTo("Value " + (LARGE_ROWS - 1));
  }

  /** Markup where innerText and a naive visibility check disagree with Selenium's getText(). */
  private static final String TRICKY_TABLE = """
      document.body.insertAdjacentHTML('beforeend', `
        <table id="tricky"><thead><tr>
          <th style="text-transform:uppercase"><svg width="10" height="10"><path d="M0 0h10v10z"/></svg> name
            <span>sort</span></th>
          <th>State</th>
        </tr></thead><tbody>
          <tr><td><svg width="10" height="10"><path d="M0 0h10v10z"/></svg> Active</td>
            <td><div style="visibility:hidden">hid<span style="visibility:visible">shown</span></div></td></tr>
          <tr><td><select><option>One</option><option>Two</option></select></td>
            <td><span style="display:contents">contents text</span></td></tr>
          <tr><td><span style="display:inline-block;width:0;height:0;overflow:hidden">ghost</span><b>real</b></td>
            <td style="text-transform:lowercase"><span>MiXeD</span> Text</td></tr>
        </tbody></table>`);
      """;

  @Test(description = "Regression guard: cell and header texts equal Selenium getText() on icons, styles and controls")
  public void snapshotTextMatchesGetTextOnTrickyMarkup() {
    openSnapshotFixture();
    Selenide.executeJavaScript(TRICKY_TABLE);
    Table table = Table.of($("#tricky"), TableLayout.html());
    table.rows().shouldHave(size(3));

    TableSnapshot snap = table.snapshot();

    assertThat(snap.headers()).isEqualTo(table.headers().texts().stream().map(TableSnapshotTest::normalize).toList());
    for (int row = 0; row < 3; row++) {
      for (int column = 0; column < 2; column++) {
        assertThat(snap.rows().get(row).cell(column).text()).as("row %d, column %d", row, column)
            .isEqualTo(normalize(table.row(row).cell(column).getText()));
      }
    }
  }

  private static String normalize(String text) {
    return text.replaceAll("[\\s\\u00A0]+", " ").strip();
  }

  private static Table chipGrid() {
    Table table = Table.of($("#chip-grid"), TableLayout.aria());
    table.rows().shouldHave(size(3));
    return table;
  }

  private static Table largeGrid() {
    Table table = Table.of($("#large-grid"), TableLayout.aria());
    table.rows().shouldHave(size(LARGE_ROWS));
    return table;
  }

  private static Table grid(String selector) {
    return Table.of($(selector), TableLayout.of(
        By.cssSelector(":scope > .data-row"), By.cssSelector(GRID_CELLS),
        By.cssSelector(":scope > .header-row > .cell:not([hidden])")));
  }
}
