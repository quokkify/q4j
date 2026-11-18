package dev.quokkify.test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableLayout;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.ElementShould;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TableShouldSatisfyTest extends BaseTest {

  private static final By CHIPS = By.cssSelector(".chip");
  private static final Duration SHORT = Duration.ofMillis(600);
  private static final int DELAY_MS = 700;
  private static final int LARGE_ROWS = 1200;

  /** Appends a "Delta" row with two chips to {@code #chip-grid} after a delay, as an async render would. */
  private static final String APPEND_ROW_LATER = """
      setTimeout(() => {
        const row = document.createElement('div');
        row.setAttribute('role', 'row');
        row.innerHTML = '<span role="gridcell">Delta</span>'
            + '<span role="gridcell"><span class="chip">Gold</span><span class="chip">Grey</span></span>'
            + '<span role="gridcell">s4</span>';
        document.getElementById('chip-grid').appendChild(row);
      }, arguments[0]);
      """;

  /** Replaces {@code #chip-grid} with a fresh copy whose first name is "Omega" after a delay (a remount). */
  private static final String REMOUNT_LATER = """
      setTimeout(() => {
        const old = document.getElementById('chip-grid');
        const fresh = old.cloneNode(true);
        fresh.querySelector('[role=gridcell]').textContent = 'Omega';
        old.replaceWith(fresh);
      }, arguments[0]);
      """;

  /** Adds a "Region" column to {@code #chip-grid} after a delay. */
  private static final String ADD_COLUMN_LATER = """
      setTimeout(() => {
        document.querySelectorAll('#chip-grid [role=row]').forEach((row, i) => {
          const cell = document.createElement('span');
          cell.setAttribute('role', i === 0 ? 'columnheader' : 'gridcell');
          cell.textContent = i === 0 ? 'Region' : 'R' + i;
          row.appendChild(cell);
        });
      }, arguments[0]);
      """;

  @Test
  public void verifiesNameToChipsMap() {
    openSnapshotFixture();

    chipGrid().shouldSatisfy(CHIPS, snap -> assertThat(snap.columnValuesBy("Name", "Properties"))
        .containsExactly(Map.entry("Alpha", List.of("Red", "Blue")), Map.entry("Beta", List.of("Green")),
            Map.entry("Gamma", List.of("Violet"))));
  }

  @Test
  public void waitsForRowsRenderedLater() {
    openSnapshotFixture();
    Selenide.executeJavaScript(APPEND_ROW_LATER, DELAY_MS);

    chipGrid().shouldSatisfy(CHIPS, snap -> assertThat(snap.columnValuesBy("Name", "Properties"))
        .hasSize(4).containsEntry("Delta", List.of("Gold", "Grey")));
  }

  @Test
  public void waitsForColumnRenderedLater() {
    openSnapshotFixture();
    Selenide.executeJavaScript(ADD_COLUMN_LATER, DELAY_MS);

    chipGrid().shouldSatisfy(snap -> assertThat(snap.column("Region")).containsExactly("R1", "R2", "R3"));
  }

  @Test
  public void survivesRootRemount() {
    openSnapshotFixture();
    Table table = chipGrid();
    Selenide.executeJavaScript(REMOUNT_LATER, DELAY_MS);

    table.shouldSatisfy(snap -> assertThat(snap.column("Name")).containsExactly("Omega", "Beta", "Gamma"));
  }

  @Test
  public void failsAfterTimeoutWithAssertionMessage() {
    openSnapshotFixture();

    assertThatThrownBy(() -> chipGrid().shouldSatisfy(
        snap -> assertThat(snap.column("Name")).as("names").containsExactly("Alpha", "Beta"), SHORT))
        .isInstanceOf(ElementShould.class)
        .hasMessageContaining("table data satisfying the assertions")
        .hasMessageContaining("chip-grid")
        .hasMessageContaining("[names]")
        .hasMessageContaining("Gamma");
  }

  @Test
  public void missingHeaderFailsAfterTimeoutNamingDisplayedHeaders() {
    openSnapshotFixture();

    assertThatThrownBy(() -> chipGrid().shouldSatisfy(snap -> snap.column("Region"), SHORT))
        .isInstanceOf(ElementShould.class)
        .hasMessageContaining("\"Region\" not found")
        .hasMessageContaining("[Name, Properties, Secret]");
  }

  @Test
  public void missingRootFailsWithElementNotFound() {
    openSnapshotFixture();
    Table table = Table.of($("#no-such-grid"), TableLayout.aria());

    assertThatThrownBy(() -> table.shouldSatisfy(snap -> assertThat(snap.rows()).isNotEmpty(), SHORT))
        .isInstanceOf(ElementNotFound.class);
  }

  @Test
  public void verifiesLargeGridFast() {
    openSnapshotFixture();
    Map<String, String> expected = new LinkedHashMap<>();
    for (int i = 0; i < LARGE_ROWS; i++) {
      expected.put("Name " + i, "Value " + i);
    }
    Table table = Table.of($("#large-grid"), TableLayout.aria());

    long start = System.nanoTime();
    table.shouldSatisfy(snap -> assertThat(snap.columnBy("Name", "Value")).containsExactlyEntriesOf(expected));
    Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

    assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
  }

  private static Table chipGrid() {
    return Table.of($("#chip-grid"), TableLayout.aria());
  }
}
