package dev.quokkify.test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableCondition;
import dev.quokkify.elements.table.TableLayout;
import dev.quokkify.elements.table.TableSnapshot;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.ElementShould;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs every example of {@code docs/table-bulk-checks.md} against the fixtures and asserts its documented result.
 * Each example is also captured as a screenshot in {@code build/reports/table-docs}: a code panel, a result panel and
 * the table with the elements involved highlighted. Copy the images to {@code docs/table-bulk-checks/} to refresh
 * the document.
 */
public class TableBulkDocExamplesTest extends BaseTest {

  private static final Path OUTPUT = Path.of("build", "reports", "table-docs");
  private static final By CHIP = By.cssSelector(".chip");
  private static final int LARGE_ROWS = 1200;
  private static final int LATE_ROW_MS = 1500;

  /** Moves the table into a frame with a code panel, a result panel and a legend; marks it ready for a screenshot. */
  private static final String DECORATE = """
      const [root, code, result, legend, maxHeight] = arguments;
      const old = document.getElementById('doc-shot');
      if (old) {
        old.parentNode.insertBefore(root, old);
        old.remove();
      }
      const shot = document.createElement('div');
      shot.id = 'doc-shot';
      shot.style.cssText = 'display:inline-block;padding:16px;background:#fff;font-family:arial,sans-serif;'
          + 'min-width:640px';
      const panel = (text, dark) => {
        const pre = document.createElement('pre');
        pre.textContent = text;
        pre.style.cssText = 'margin:0 0 10px;padding:10px 14px;border-radius:6px;font:13px/1.45 monospace;'
            + 'white-space:pre;' + (dark ? 'background:#0f172a;color:#e2e8f0' : 'background:#f1f5f9;color:#0f172a;'
            + 'border:1px solid #cbd5e1');
        return pre;
      };
      shot.appendChild(panel(code, true));
      if (result) {
        shot.appendChild(panel(result, false));
      }
      if (legend.length) {
        const box = document.createElement('div');
        box.style.cssText = 'margin:0 0 10px;font-size:12px;color:#334155';
        for (const [color, text] of legend) {
          const item = document.createElement('span');
          item.style.cssText = 'margin-right:16px;white-space:nowrap';
          item.innerHTML = '<span style="display:inline-block;width:12px;height:12px;vertical-align:-2px;'
              + 'margin-right:6px;border:2px solid ' + color + '"></span>';
          item.appendChild(document.createTextNode(text));
          box.appendChild(item);
        }
        shot.appendChild(box);
      }
      const frame = document.createElement('div');
      if (maxHeight) {
        frame.style.cssText = 'max-height:' + maxHeight + 'px;overflow:hidden';
      }
      root.parentNode.insertBefore(shot, root);
      frame.appendChild(root);
      shot.appendChild(frame);
      root.style.margin = '0';
      """;

  /** Outlines every element matching a CSS selector inside the root. */
  private static final String OUTLINE = """
      const [root, selector, color, dashed] = arguments;
      root.querySelectorAll(selector).forEach(el => {
        el.style.outline = '3px ' + (dashed ? 'dashed ' : 'solid ') + color;
        el.style.outlineOffset = '-3px';
      });
      """;

  private static final String BLUE = "#2563eb";
  private static final String RED = "#dc2626";
  private static final String AMBER = "#d97706";
  private static final String GREEN = "#16a34a";
  private static final String GREY = "#64748b";

  private static final String DATA_ROWS = "[role=row]:has([role=gridcell])";
  private static final String HEADERS = "[role=columnheader]";
  private static final String VISIBLE_CHIPS = ".chip:not([hidden]):not([style*='visibility']):not([style*='opacity'])";

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

  @Test(description = "Docs 1-4: snapshot, items, hidden elements, keyed maps")
  public void readExamples() {
    openSnapshotFixture();
    Table grid = chipGrid();

    TableSnapshot snap = grid.snapshot();
    assertThat(snap.headers()).containsExactly("Name", "Properties", "Secret");
    assertThat(snap.column("Name")).containsExactly("Alpha", "Beta", "Gamma");
    shot(grid, "01-snapshot", """
        TableSnapshot snap = grid.snapshot();     // one browser call
        snap.headers();                           // amber
        snap.column("Name");                      // blue rows, first column""",
        "headers()      = " + snap.headers() + "\ncolumn(\"Name\") = " + snap.column("Name"),
        List.of(List.of(AMBER, "headers()"), List.of(BLUE, "rows()")), 0,
        new String[] {HEADERS, AMBER}, new String[] {DATA_ROWS, BLUE});

    TableSnapshot chips = grid.snapshot(CHIP);
    assertThat(chips.columnValues("Properties")).containsExactly(List.of("Red", "Blue"), List.of("Green"),
        List.of("Violet"));
    shot(grid, "02-items", """
        TableSnapshot snap = grid.snapshot(By.cssSelector(".chip"));
        snap.column("Properties");        // cell text
        snap.columnValues("Properties");  // red: items found inside each cell""",
        "column(\"Properties\")       = " + chips.column("Properties")
            + "\ncolumnValues(\"Properties\") = " + chips.columnValues("Properties"),
        List.of(List.of(RED, "items read by the .chip locator")), 0,
        new String[] {".chip:not([hidden])", RED});

    assertThat(chips.column("Secret")).containsExactly("", "s2", "");
    assertThat(grid.row(0).cell("Secret").text()).isEmpty();
    shot(grid, "03-hidden", """
        grid.snapshot(By.cssSelector(".chip")).column("Secret");
        // row 1: display:none, row 3: opacity:0 -> "" (same as getText() of the lazy API)
        // row 2 chips: hidden, visibility:hidden and opacity:0 chips are skipped""",
        "column(\"Secret\")             = " + chips.column("Secret")
            + "\ncolumnValues(\"Properties\") = " + chips.columnValues("Properties"),
        List.of(List.of(GREY, "cell with hidden, invisible and transparent chips"), List.of(RED, "items returned")),
        0, new String[] {"[role=gridcell]:has(> [hidden])", GREY, "dashed"},
        new String[] {VISIBLE_CHIPS, RED});

    Map<String, List<String>> byName = chips.columnValuesBy("Name", "Properties");
    assertThat(byName).containsExactly(Map.entry("Alpha", List.of("Red", "Blue")), Map.entry("Beta",
        List.of("Green")), Map.entry("Gamma", List.of("Violet")));
    shot(grid, "04-keyed-map", """
        Map<String, List<String>> chips = grid.snapshot(By.cssSelector(".chip"))
            .columnValuesBy("Name", "Properties");   // key column -> items, in row order""",
        "columnValuesBy(\"Name\", \"Properties\") = " + byName,
        List.of(List.of(BLUE, "key cells (Name)"), List.of(RED, "values")), 0,
        new String[] {DATA_ROWS + " > :first-child", BLUE}, new String[] {VISIBLE_CHIPS, RED});
  }

  @Test(description = "Docs 5-7: whole-table checks wait, report a short diff and compare aligned rows")
  public void checkExamples() {
    openSnapshotFixture();
    Selenide.executeJavaScript(APPEND_ROW_LATER, LATE_ROW_MS);
    Table grid = chipGrid();
    Map<String, List<String>> expected = new LinkedHashMap<>();
    expected.put("Alpha", List.of("Red", "Blue"));
    expected.put("Beta", List.of("Green"));
    expected.put("Gamma", List.of("Violet"));
    expected.put("Delta", List.of("Gold", "Grey"));
    String code = """
        grid.shouldHave(TableCondition.exactValuesBy("Name", "Properties", By.cssSelector(".chip"), Map.of(
            "Alpha", List.of("Red", "Blue"),
            "Beta",  List.of("Green"),
            "Gamma", List.of("Violet"),
            "Delta", List.of("Gold", "Grey"))));   // Delta is rendered 1.5 s after page load""";
    TableCondition condition = TableCondition.exactValuesBy("Name", "Properties", CHIP, expected);
    String firstAttempt = failureOf(() -> grid.shouldHave(condition, Duration.ofMillis(1)));
    assertThat(firstAttempt).contains("missing (1):").contains("Delta");
    shot(grid, "05a-wait-before", code, firstAttempt + "\n... not ready yet: read again on the next poll",
        List.of(List.of(BLUE, "rows read in this attempt")), 0, new String[] {DATA_ROWS, BLUE});

    grid.shouldHave(condition);
    shot(grid, "05b-wait-after", code, "passed: 4 rows match",
        List.of(List.of(GREEN, "rows that matched")), 0, new String[] {DATA_ROWS, GREEN});

    Map<String, List<String>> wrong = new LinkedHashMap<>();
    wrong.put("Alpha", List.of("Red"));
    wrong.put("Beta", List.of("Green"));
    wrong.put("Gamma", List.of("Violet"));
    wrong.put("Epsilon", List.of());
    String diff = failureOf(() -> grid.shouldHave(
        TableCondition.exactValuesBy("Name", "Properties", CHIP, wrong), Duration.ofSeconds(1)));
    assertThat(diff).contains("missing (1):").contains("Epsilon").contains("unexpected (1):").contains("Delta")
        .contains("Alpha: expected [Red] but was [Red, Blue]");
    shot(grid, "06-failure-diff", """
        grid.shouldHave(TableCondition.exactValuesBy("Name", "Properties", By.cssSelector(".chip"), Map.of(
            "Alpha", List.of("Red"), "Beta", List.of("Green"), "Gamma", List.of("Violet"),
            "Epsilon", List.of())), Duration.ofSeconds(1));""", diff,
        List.of(List.of(RED, "different"), List.of(AMBER, "unexpected")), 0,
        new String[] {DATA_ROWS + ":nth-child(2)", RED}, new String[] {DATA_ROWS + ":last-child", AMBER});

    grid.shouldHave(TableCondition.exactRows(List.of("Name", "Secret"), List.of(
        List.of("Alpha", ""), List.of("Beta", "s2"), List.of("Gamma", ""), List.of("Delta", "s4"))));
    shot(grid, "07-exact-rows", """
        grid.shouldHave(TableCondition.exactRows(List.of("Name", "Secret"), List.of(
            List.of("Alpha", ""), List.of("Beta", "s2"), List.of("Gamma", ""), List.of("Delta", "s4"))));
        // several columns, compared row by row in row order""", "passed",
        List.of(List.of(GREEN, "compared columns")), 0,
        new String[] {DATA_ROWS + " > :first-child, " + DATA_ROWS + " > :nth-child(3)", GREEN});
  }

  @Test(description = "Docs 8-9: AssertJ checks and a 1200-row grid")
  public void assertjAndLargeExamples() {
    openSnapshotFixture();
    Table grid = chipGrid();
    grid.shouldSatisfy(CHIP, snap -> {
      assertThat(snap.column("Name")).doesNotContain("Failed").hasSize(3);
      assertThat(snap.columnValuesBy("Name", "Properties").get("Alpha")).contains("Red");
    });
    shot(grid, "08-should-satisfy", """
        grid.shouldSatisfy(By.cssSelector(".chip"), snap -> {
          assertThat(snap.column("Name")).doesNotContain("Failed").hasSize(3);
          assertThat(snap.columnValuesBy("Name", "Properties").get("Alpha")).contains("Red");
        });   // re-read and re-asserted until it passes or the timeout expires""", "passed",
        List.of(List.of(GREEN, "values the assertions looked at")), 0,
        new String[] {DATA_ROWS + " > :first-child", GREEN}, new String[] {DATA_ROWS + ":nth-child(2) .chip", GREEN});

    Table large = Table.of($("#large-grid"), TableLayout.aria());
    large.rows().shouldHave(size(LARGE_ROWS));
    long start = System.nanoTime();
    Map<String, String> values = large.snapshot().columnBy("Name", "Value");
    long millis = Duration.ofNanos(System.nanoTime() - start).toMillis();
    assertThat(values).hasSize(LARGE_ROWS).containsEntry("Name 1199", "Value 1199");
    shot(large, "09-large-grid", """
        Map<String, String> values = Table.of($("#large-grid"), TableLayout.aria())
            .snapshot().columnBy("Name", "Value");""",
        "1200 rows read in one browser call, " + millis + " ms on this run\nvalues.get(\"Name 1199\") = "
            + values.get("Name 1199"),
        List.of(List.of(BLUE, "rows read (first rows shown)")), 320, new String[] {DATA_ROWS, BLUE});
  }

  private static Table chipGrid() {
    Table table = Table.of($("#chip-grid"), TableLayout.aria());
    table.rows().shouldHave(size(3));
    return table;
  }

  private static String failureOf(Runnable check) {
    try {
      check.run();
    } catch (ElementShould e) {
      String message = e.getMessage();
      return message.substring(message.indexOf("Actual value: ")).lines()
          .takeWhile(line -> !line.startsWith("Screenshot") && !line.startsWith("Page source")
              && !line.startsWith("Timeout"))
          .collect(Collectors.joining("\n")).strip();
    }
    throw new AssertionError("the check was expected to fail");
  }

  private static void shot(Table table, String name, String code, String result, List<List<String>> legend,
      int maxHeight, String[]... outlines) {
    Selenide.executeJavaScript(DECORATE, table.root(), code, result, legend, maxHeight);
    for (String[] outline : outlines) {
      Selenide.executeJavaScript(OUTLINE, table.root(), outline[0], outline[1], outline.length > 2);
    }
    File png = $("#doc-shot").getScreenshotAs(OutputType.FILE);
    try {
      Files.createDirectories(OUTPUT);
      Files.copy(png.toPath(), OUTPUT.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new UncheckedIOException("cannot save screenshot " + name, e);
    }
    Selenide.executeJavaScript("arguments[0].querySelectorAll('*').forEach(el => el.style.outline = '');",
        table.root());
  }
}
