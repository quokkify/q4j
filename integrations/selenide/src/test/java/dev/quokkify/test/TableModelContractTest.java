package dev.quokkify.test;

import java.time.Duration;
import java.util.stream.IntStream;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.elements.table.HorizontalTable;
import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableColumnException;
import dev.quokkify.elements.table.TableLayout;
import dev.quokkify.elements.table.TableRow;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.UIAssertionError;
import org.openqa.selenium.By;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TableModelContractTest extends BaseTest {

  private static final String REPETITIONS_PROPERTY = "tableModel.contract.repetitions";
  private static final Duration LOOKUP_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration SHORT_TIMEOUT = Duration.ofMillis(100);
  private static final String GRID_CELLS = ":scope > .cell:not([hidden])";

  @Test(description = "Resolve typed columns by displayed headers rather than enum ordinal")
  public void resolvesDisplayedHeader() {
    openClassicVariantsFixture();
    Table classic = classic("#classic");
    Table dynamic = classic("#dynamic");

    classic.row(0).cell("Company").shouldHave(exactText("Alfreds"), LOOKUP_TIMEOUT);
    classic.row(0).cell(1).shouldHave(exactText("Alfreds"));
    dynamic.row(0).cell("Company").shouldHave(exactText("Alfreds"), LOOKUP_TIMEOUT);
    dynamic.row(0).cell(0).shouldHave(exactText("Alfreds"));
  }

  @Test(description = "Missing typed columns fail with the requested and available displayed headers")
  public void reportsMissingDisplayedHeader() {
    openClassicVariantsFixture();
    Table classic = classic("#classic");

    classic.headers().shouldHave(exactTexts("Country", "Company"));
    assertThatThrownBy(() -> classic.row(0).cell("Region"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("Region")
        .hasMessageContaining("[Country, Company]");
  }

  @Test(description = "Reject typed columns whose displayed header occurs more than once")
  public void rejectsAmbiguousDisplayedHeader() {
    openEdgeCasesFixture();
    Table repeated = classic("#repeated-table");

    repeated.headers().shouldHave(exactTexts("Country", "Company", "Company"));
    repeated.row(0).cell("Country").shouldHave(exactText("Austria"));
    assertThatThrownBy(() -> repeated.row(0).cell("Company"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("ambiguous");
  }

  @Test(description = "Rows expose typed lazy-cell contract without table capabilities")
  public void readsTypedCellLazily() {
    openClassicVariantsFixture();

    classic("#classic").row(0).cell("Country").shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
  }

  @Test(description = "The structural table contract supports a backend without browser types")
  public void supportsFullyCustomBackendContract() {
    openCustomGridsFixture();
    Table grid = grid("#custom-grid");
    TableRow outer = grid.row(0);

    grid.rows().shouldHave(size(3));
    outer.cell("Country").shouldHave(exactText("Austria"));
    assertThat(outer.cell("Company").shouldBe(exist).text())
        .startsWith("Outer")
        .contains("Leak");
  }

  @Test(description = "Every legacy table variant exposes the neutral model and typed cells")
  public void bridgesAllLegacyVariants() {
    openClassicVariantsFixture();
    Table flex = flex();

    classic("#classic").row(0).cell("Country").shouldHave(exactText("Austria"));
    classic("#dynamic").headers().shouldHave(exactTexts("Company", "Country"));
    flex.headers().shouldHave(exactTexts("Country", "Company"));
    flex.row(0).cell("Company").shouldHave(exactText("Alfreds"));
    HorizontalTable.of($("#horizontal")).value("Country").shouldHave(exactText("Austria"));
    HorizontalTable.of($("#dynamic-horizontal")).value("Country").shouldHave(exactText("Austria"));
  }

  @DataProvider(name = "tableModelContractIterations", parallel = false)
  public Object[][] tableModelContractIterations() {
    int repetitions = Integer.parseInt(System.getProperty(REPETITIONS_PROPERTY, "1"));
    return IntStream.rangeClosed(1, repetitions)
        .mapToObj(iteration -> new Object[] {"iteration-%02d".formatted(iteration)})
        .toArray(Object[][]::new);
  }

  @Test(dataProvider = "tableModelContractIterations",
      description = "Required lookup waits for a row restored asynchronously")
  @SingleThread
  public void waitsForDelayedRow(String iteration) {
    openClassicVariantsFixture();
    Table classic = classic("#classic");
    Selenide.executeJavaScript("window.prepareDelayedRow()");
    classic.rows().shouldHave(empty);
    Selenide.executeJavaScript("window.restoreDelayedRow()");

    classic.row("Company", "Alfreds").cell("Country").shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
  }

  @Test(dataProvider = "tableModelContractIterations",
      description = "A row reference resolves again after a deterministic DOM remount")
  @SingleThread
  public void rowReferenceSurvivesRemount(String iteration) {
    openClassicVariantsFixture();
    TableRow row = classic("#classic").row("Company", "Alfreds");
    row.self().shouldBe(visible, LOOKUP_TIMEOUT);
    Selenide.executeJavaScript("window.remount()");

    row.cell("Country").shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
  }

  @Test(description = "Required row handles skip CLASSIC and FLEX header rows")
  public void requiredRowsSkipHeaders() {
    openClassicVariantsFixture();
    TableRow classicRow = classic("#classic").row(0);
    TableRow flexRow = flex().row(0);

    classicRow.cell(0).shouldHave(exactText("Austria"), SHORT_TIMEOUT);
    flexRow.cell(0).shouldHave(exactText("Austria"), SHORT_TIMEOUT);
    Selenide.executeJavaScript("window.remount()");
    classicRow.cell(0).shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
    flexRow.cell(0).shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
  }

  @Test(description = "Optional and required lookups distinguish missing rows and cells")
  public void reportsMissingRowsAndCellsConsistently() {
    openClassicVariantsFixture();
    Table classic = classic("#classic");
    TableRow row = classic.row(0);

    row.cell(0).shouldBe(exist);
    Selenide.executeJavaScript("window.prepareMissingCell()");
    row.cell(1).shouldNot(exist, SHORT_TIMEOUT);
    assertThatThrownBy(() -> row.cell(1).shouldBe(exist, SHORT_TIMEOUT))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("td");
    assertThatThrownBy(() -> classic.row("Company", "missing").self().should(exist, SHORT_TIMEOUT))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("missing");
  }

  @Test(description = "Required lookup waits for a table root mounted after the initial DOM")
  public void waitsForLateRootMount() {
    openClassicVariantsFixture();
    Table classic = classic("#classic");

    Selenide.executeJavaScript("window.prepareLateMount()");
    classic.root().shouldNot(exist, SHORT_TIMEOUT);

    classic.row("Company", "Alfreds").cell(0).shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
  }

  @Test(description = "Required lookup applies one timeout across late root and row discovery")
  public void timesOutAcrossLateRootAndRowDiscovery() {
    openClassicVariantsFixture();

    Selenide.executeJavaScript("""
        const table = document.getElementById('classic');
        const row = table.querySelector('tbody tr');
        row.remove();
        table.remove();
        window.setTimeout(() => document.body.prepend(table), 250);
        window.setTimeout(() => table.querySelector('tbody').appendChild(row), 650);
        """);

    assertThatThrownBy(() -> classic("#classic").row("Company", "Alfreds").self()
        .should(exist, Duration.ofMillis(450)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Alfreds")
        .hasMessageContaining("450ms");
  }

  @Test(description = "Public custom adapter isolates nested grids and preserves logical cells")
  public void supportsCustomDivAdapter() {
    openCustomGridsFixture();
    Table grid = grid("#custom-grid");
    Table nested = grid("#nested-custom-grid");

    grid.headers().shouldHave(exactTexts("Country", "Company"));
    grid.rows().shouldHave(size(3));
    grid.row(0).cell("Company").shouldHave(text("Outer"));
    assertThat(grid.row(1).cell("Company").shouldBe(exist).text()).isEmpty();
    grid.row(2).cells().shouldHave(size(1));
    grid.row(2).cell("Company").shouldNot(exist, SHORT_TIMEOUT);
    nested.rows().shouldHave(size(1));
    nested.row(0).cell("Company").shouldHave(exactText("Leak"));
  }

  @Test(description = "Custom adapter waits once for a late root and row, then remount-safe handles resolve")
  @SingleThread
  public void customAdapterWaitsAndSurvivesRemount() {
    openCustomGridsFixture();
    Table grid = grid("#custom-grid");
    TableRow row = grid.row(0);
    Selenide.executeJavaScript("window.remountCustomGrid()");

    row.cell("Country").shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);

    Selenide.executeJavaScript("window.prepareCustomDelayed()");
    grid.row("Country", "Austria").cell("Country").shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
  }

  @Test(description = "Classic adapter preserves tables whose header row is inside tbody")
  public void supportsBodyOnlyClassicTable() {
    openEdgeCasesFixture();
    Table table = Table.of($("#body-only-classic"), TableLayout.of(
        By.xpath("./tbody/tr[td]"), By.xpath("./*[self::td or self::th]"), By.xpath("./tbody/tr[1]/th")));

    table.headers().shouldHave(exactTexts("Country", "Company"));
    table.rows().shouldHave(size(1));
    table.row(0).cell("Company").shouldHave(exactText("Alfreds"));
    table.row(0).cell("Country").shouldHave(exactText("Austria"));
  }

  @Test(description = "Classic adapter excludes rows belonging to a nested table")
  public void excludesNestedTableRows() {
    openEdgeCasesFixture();
    Table table = classic("#nested-classic");

    table.rows().shouldHave(size(1));
    table.row(0).cell("Company").shouldHave(text("Outer"));
  }

  @Test(description = "Classic adapter can select a nested table as its own model root")
  public void supportsNestedClassicRoot() {
    openEdgeCasesFixture();
    Table table = classic("#nested-classic table");

    table.rows().shouldHave(size(1));
    table.row(0).cell("Country").shouldHave(exactText("Nested"));
    table.row(0).cell("Company").shouldHave(exactText("Leak"));
  }

  @Test(description = "Generic ARIA adapter addresses role-based grids and survives root remount")
  public void supportsAriaGridAndRemount() {
    openCustomGridsFixture();
    Table grid = Table.of($("#aria-grid"), TableLayout.aria());
    TableRow row = grid.row("Company", "Alfreds");

    grid.headers().shouldHave(exactTexts("Country", "Company"));
    row.cell("Company").shouldHave(exactText("Alfreds"));
    Selenide.executeJavaScript("window.remountAriaGrid()");
    row.cell("Country").shouldHave(exactText("Austria"));
  }

  @Test(description = "Headerless and repeated headers preserve typed lookup failures")
  public void handlesHeaderlessAndRepeatedHeaders() {
    openCustomGridsFixture();
    Table headerless = Table.of($("#headerless-grid"), TableLayout.of(
        By.cssSelector(":scope > .data-row"), By.cssSelector(":scope > .cell"),
        By.cssSelector(":scope > .header-row > .cell")));
    Table repeated = grid("#custom-repeated-grid");

    headerless.headers().shouldHave(empty);
    assertThatThrownBy(() -> headerless.row(0).cell("Country"))
        .isInstanceOf(UIAssertionError.class);
    repeated.headers().shouldHave(exactTexts("Country", "Company", "Company", "Company"));
    repeated.rows().shouldHave(size(3));
    assertThatThrownBy(() -> repeated.row(0).cell("Company"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("ambiguous");
  }

  private static Table classic(String selector) {
    return Table.of($(selector), TableLayout.html());
  }

  private static Table flex() {
    return Table.of($("#flex"), TableLayout.of(
        By.cssSelector(":scope > .flex-table-row ~ .flex-table-row"), By.cssSelector(":scope > div"),
        By.cssSelector(":scope > [data-flex-preamble] + .flex-table-row > div")));
  }

  private static Table grid(String selector) {
    return Table.of($(selector), TableLayout.of(
        By.cssSelector(":scope > .data-row"), By.cssSelector(GRID_CELLS),
        By.cssSelector(":scope > .header-row > .cell:not([hidden])")));
  }
}
