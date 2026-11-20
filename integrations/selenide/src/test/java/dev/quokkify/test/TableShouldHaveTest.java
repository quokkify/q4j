package dev.quokkify.test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import dev.quokkify.annotation.Sequential;
import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableCondition;
import dev.quokkify.elements.table.TableLayout;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.ElementShould;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TableShouldHaveTest extends BaseTest {

  private static final By CHIP = By.cssSelector(".chip");
  private static final Duration SHORT = Duration.ofMillis(500);
  private static final int LARGE_ROWS = 1200;
  private static final int CHIP_ROWS = 400;

  // Issue #895 shape: an ARIA grid whose "Properties" cells hold chips, built on the snapshot fixture page.
  private static final String BUILD_CHIP_GRID = """
      const grid = document.createElement('div');
      grid.id = 'issue-grid';
      grid.setAttribute('role', 'grid');
      const header = document.createElement('div');
      header.setAttribute('role', 'row');
      header.innerHTML = '<span role="columnheader">Name</span><span role="columnheader">Properties</span>';
      grid.appendChild(header);
      for (let i = 0; i < arguments[0]; i++) {
        const row = document.createElement('div');
        row.setAttribute('role', 'row');
        row.innerHTML = '<span role="gridcell">Item ' + i + '</span><span role="gridcell" class="chips-flex">'
            + '<span class="chip">P' + i + '</span><span class="chip">Q' + (i % 7) + '</span></span>';
        grid.appendChild(row);
      }
      document.body.appendChild(grid);
      """;

  @Test
  public void chipGridMatchesChipsByName() {
    openSnapshotFixture();

    chipGrid().shouldHave(TableCondition.exactItemsBy("Name", "Properties", CHIP, Map.of(
        "Gamma", List.of("Violet"),
        "Alpha", List.of("Red", "Blue"),
        "Beta", List.of("Green"))));
  }

  @Test
  public void waitsForLateChip() {
    openSnapshotFixture();
    Selenide.executeJavaScript("setTimeout(() => {"
        + " const chip = document.createElement('span'); chip.className = 'chip'; chip.textContent = 'Yellow';"
        + " document.querySelector('#chip-grid .chips-flex').appendChild(chip); }, 800)");

    chipGrid().shouldHave(TableCondition.exactItemsBy("Name", "Properties", CHIP, Map.of(
        "Alpha", List.of("Red", "Blue", "Yellow"),
        "Beta", List.of("Green"),
        "Gamma", List.of("Violet"))));
  }

  @Test
  public void waitsForLateRows() {
    openSnapshotFixture();
    Selenide.executeJavaScript("const [source, rows] = arguments; setTimeout(() => new Function(source)(rows), 800);",
        BUILD_CHIP_GRID, 3);
    Table grid = Table.of($("#issue-grid"), TableLayout.aria());

    grid.shouldHave(TableCondition.exactRows(List.of("Name"),
        List.of(List.of("Item 0"), List.of("Item 1"), List.of("Item 2"))));
  }

  @Test
  public void mismatchFailsWithShortDifference() {
    openSnapshotFixture();

    assertThatThrownBy(() -> chipGrid().shouldHave(TableCondition.exactItemsBy("Name", "Properties", CHIP, Map.of(
        "Alpha", List.of("Red"),
        "Beta", List.of("Green"),
        "Delta", List.of())), SHORT))
        .isInstanceOf(ElementShould.class)
        .hasMessageContaining("\"Properties\" items by \"Name\" (3 rows)")
        .hasMessageContaining("missing (1):\n  Delta")
        .hasMessageContaining("unexpected (1):\n  Gamma")
        .hasMessageContaining("Alpha: expected [Red] but was [Red, Blue]");
  }

  @Test
  public void missingHeaderFailsAfterTimeoutWithDisplayedHeaders() {
    openSnapshotFixture();

    assertThatThrownBy(() -> chipGrid().shouldHave(
        TableCondition.exactRows(List.of("Title"), List.of()), SHORT))
        .isInstanceOf(ElementShould.class)
        .hasMessageContaining("\"Title\"")
        .hasMessageContaining("not found")
        .hasMessageContaining("[Name, Properties, Secret]");
  }

  @Test
  public void hiddenCellsAndItemsReadAsInSnapshot() {
    openSnapshotFixture();

    chipGrid().shouldHave(TableCondition.exactRows(List.of("Name", "Secret"),
        List.of(List.of("Alpha", ""), List.of("Beta", "s2"), List.of("Gamma", ""))));
  }

  @Test
  @Sequential
  public void issueSizedChipGridVerifiesFast() {
    openSnapshotFixture();
    Selenide.executeJavaScript(BUILD_CHIP_GRID, CHIP_ROWS);
    Map<String, List<String>> expected = new LinkedHashMap<>();
    IntStream.range(0, CHIP_ROWS).forEach(i -> expected.put("Item " + i, List.of("P" + i, "Q" + (i % 7))));
    Table grid = Table.of($("#issue-grid"), TableLayout.aria());

    long start = System.nanoTime();
    grid.shouldHave(TableCondition.exactItemsBy("Name", "Properties", CHIP, expected));
    Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

    assertThat(elapsed).isLessThan(Duration.ofSeconds(10));
  }

  @Test
  @Sequential
  public void largeGridExactRowsVerifiesFast() {
    openSnapshotFixture();
    List<List<String>> expected = IntStream.range(0, LARGE_ROWS)
        .mapToObj(i -> List.of("Name " + i, "Value " + i)).toList();
    Table grid = Table.of($("#large-grid"), TableLayout.aria());

    long start = System.nanoTime();
    grid.shouldHave(TableCondition.exactRows(List.of("Name", "Value"), expected));
    Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

    assertThat(elapsed).isLessThan(Duration.ofSeconds(10));
  }

  private static Table chipGrid() {
    return Table.of($("#chip-grid"), TableLayout.aria());
  }
}
