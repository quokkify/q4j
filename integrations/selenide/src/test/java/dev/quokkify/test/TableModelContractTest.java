package dev.quokkify.test;

import java.time.Duration;
import java.util.stream.IntStream;

import dev.quokkify.annotation.SingleThread;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.UIAssertionError;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byRole;
import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
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

    assertThat(columnIndex(classicHeaders($("#classic")), "Company")).isEqualTo(1);
    assertThat(columnIndex(classicHeaders($("#dynamic")), "Company")).isEqualTo(0);
  }

  @Test(description = "Missing typed columns fail with the requested and available displayed headers")
  public void reportsMissingDisplayedHeader() {
    openClassicVariantsFixture();
    ElementsCollection headers = classicHeaders($("#classic"));

    headers.shouldHave(exactTexts("Country", "Company"));
    assertThatThrownBy(() -> headers.shouldHave(itemWithText("Region"), SHORT_TIMEOUT))
        .isInstanceOf(UIAssertionError.class)
        .hasMessageContaining("Region");
  }

  @Test(description = "Reject typed columns whose displayed header occurs more than once")
  public void rejectsAmbiguousDisplayedHeader() {
    openEdgeCasesFixture();

    assertThat(classicHeaders($("#repeated-table")).shouldHave(size(3)).texts())
        .containsExactly("Country", "Company", "Company")
        .containsOnlyOnce("Country");
  }

  @Test(description = "Rows expose typed lazy-cell contract without table capabilities")
  public void readsTypedCellLazily() {
    openClassicVariantsFixture();
    SelenideElement country = classicCells(classicRows($("#classic")).first())
        .get(columnIndex(classicHeaders($("#classic")), "Country"));

    country.shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
  }

  @Test(description = "The structural table contract supports a backend without browser types")
  public void supportsFullyCustomBackendContract() {
    openCustomGridsFixture();
    ElementsCollection rows = gridRows($("#custom-grid"));
    int company = columnIndex(gridHeaders($("#custom-grid")), "Company");
    SelenideElement outer = rows.findBy(text("Outer"));

    rows.shouldHave(size(3));
    gridCells(outer).get(columnIndex(gridHeaders($("#custom-grid")), "Country"))
        .shouldHave(exactText("Austria"));
    assertThat(gridCells(outer).get(company).shouldBe(exist).text())
        .startsWith("Outer")
        .contains("Leak");
  }

  @Test(description = "Every legacy table variant exposes the neutral model and typed cells")
  public void bridgesAllLegacyVariants() {
    openClassicVariantsFixture();

    classicCells(classicRows($("#classic")).first())
        .get(columnIndex(classicHeaders($("#classic")), "Country"))
        .shouldHave(exactText("Austria"));
    classicHeaders($("#dynamic")).shouldHave(exactTexts("Company", "Country"));
    ElementsCollection flexRows = $$("#flex > .flex-table-row");
    flexRows.first().$$(":scope > div").shouldHave(exactTexts("Country", "Company"));
    flexRows.get(1).$$(":scope > div").get(1).shouldHave(exactText("Alfreds"));
    $$("#horizontal tr").findBy(text("Country")).$("td").shouldHave(exactText("Austria"));
    $$("#dynamic-horizontal tr").findBy(text("Country")).$("td").shouldHave(exactText("Austria"));
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
    int country = columnIndex(classicHeaders($("#classic")), "Country");
    Selenide.executeJavaScript("window.prepareDelayedRow()");
    classicRows($("#classic")).shouldHave(empty);
    Selenide.executeJavaScript("window.restoreDelayedRow()");

    classicCells(classicRows($("#classic")).findBy(text("Alfreds"))).get(country)
        .shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
  }

  @Test(dataProvider = "tableModelContractIterations",
      description = "A row reference resolves again after a deterministic DOM remount")
  @SingleThread
  public void rowReferenceSurvivesRemount(String iteration) {
    openClassicVariantsFixture();
    int country = columnIndex(classicHeaders($("#classic")), "Country");
    SelenideElement row = classicRows($("#classic")).findBy(text("Alfreds"));
    row.shouldBe(visible, LOOKUP_TIMEOUT);
    Selenide.executeJavaScript("window.remount()");

    classicCells(row).get(country).shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
  }

  @Test(description = "Required row handles skip CLASSIC and FLEX header rows")
  public void requiredRowsSkipHeaders() {
    openClassicVariantsFixture();
    SelenideElement classicRow = classicRows($("#classic")).first();
    SelenideElement flexRow = $$("#flex > .flex-table-row").excludeWith(text("Country")).first();

    classicCells(classicRow).get(0).shouldHave(exactText("Austria"), SHORT_TIMEOUT);
    flexRow.$$(":scope > div").get(0).shouldHave(exactText("Austria"), SHORT_TIMEOUT);
    Selenide.executeJavaScript("window.remount()");
    classicCells(classicRow).get(0).shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
    flexRow.$$(":scope > div").get(0).shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
  }

  @Test(description = "Optional and required lookups distinguish missing rows and cells")
  public void reportsMissingRowsAndCellsConsistently() {
    openClassicVariantsFixture();
    SelenideElement row = classicRows($("#classic")).first();

    classicCells(row).get(0).shouldBe(exist);
    Selenide.executeJavaScript("window.prepareMissingCell()");
    classicCells(row).get(1).shouldNot(exist, SHORT_TIMEOUT);
    assertThatThrownBy(() -> classicCells(row).get(1).shouldBe(exist, SHORT_TIMEOUT))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("td");
    assertThatThrownBy(() -> classicRows($("#classic")).findBy(text("missing")).should(exist, SHORT_TIMEOUT))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("missing");
  }

  @Test(description = "Required lookup waits for a table root mounted after the initial DOM")
  public void waitsForLateRootMount() {
    openClassicVariantsFixture();

    Selenide.executeJavaScript("window.prepareLateMount()");
    $("#classic").shouldNot(exist, SHORT_TIMEOUT);

    classicCells(classicRows($("#classic")).findBy(text("Alfreds"))).get(0)
        .shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
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

    assertThatThrownBy(() -> classicRows($("#classic")).findBy(text("Alfreds"))
        .should(exist, Duration.ofMillis(450)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Alfreds")
        .hasMessageContaining("450ms");
  }

  @Test(description = "Public custom adapter isolates nested grids and preserves logical cells")
  public void supportsCustomDivAdapter() {
    openCustomGridsFixture();
    SelenideElement grid = $("#custom-grid");
    SelenideElement nested = $("#nested-custom-grid");
    ElementsCollection rows = gridRows(grid);
    int company = columnIndex(gridHeaders(grid), "Company");

    gridHeaders(grid).shouldHave(exactTexts("Country", "Company"));
    rows.shouldHave(size(3));
    gridCells(rows.get(0)).get(company).shouldHave(text("Outer"));
    assertThat(gridCells(rows.get(1)).get(company).shouldBe(exist).text()).isEmpty();
    gridCells(rows.get(2)).shouldHave(size(1));
    gridCells(rows.get(2)).get(company).shouldNot(exist, SHORT_TIMEOUT);
    gridRows(nested).shouldHave(size(1));
    gridCells(gridRows(nested).first()).get(columnIndex(gridHeaders(nested), "Company"))
        .shouldHave(exactText("Leak"));
  }

  @Test(description = "Custom adapter waits once for a late root and row, then remount-safe handles resolve")
  @SingleThread
  public void customAdapterWaitsAndSurvivesRemount() {
    openCustomGridsFixture();
    int country = columnIndex(gridHeaders($("#custom-grid")), "Country");
    SelenideElement row = gridRows($("#custom-grid")).first();
    Selenide.executeJavaScript("window.remountCustomGrid()");

    gridCells(row).get(country).shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);

    Selenide.executeJavaScript("window.prepareCustomDelayed()");
    gridCells(gridRows($("#custom-grid")).findBy(text("Austria"))).get(country)
        .shouldHave(exactText("Austria"), LOOKUP_TIMEOUT);
  }

  @Test(description = "Classic adapter preserves tables whose header row is inside tbody")
  public void supportsBodyOnlyClassicTable() {
    openEdgeCasesFixture();
    SelenideElement table = $("#body-only-classic");
    ElementsCollection headers = table.$$(byXpath("./tbody/tr[1]/th"));
    ElementsCollection rows = table.$$(byXpath("./tbody/tr[td]"));

    headers.shouldHave(exactTexts("Country", "Company"));
    rows.shouldHave(size(1));
    classicCells(rows.first()).get(columnIndex(headers, "Company")).shouldHave(exactText("Alfreds"));
    classicCells(rows.first()).get(columnIndex(headers, "Country")).shouldHave(exactText("Austria"));
  }

  @Test(description = "Classic adapter excludes rows belonging to a nested table")
  public void excludesNestedTableRows() {
    openEdgeCasesFixture();
    SelenideElement table = $("#nested-classic");
    ElementsCollection rows = classicRows(table);

    rows.shouldHave(size(1));
    classicCells(rows.first()).get(columnIndex(classicHeaders(table), "Company"))
        .shouldHave(text("Outer"));
  }

  @Test(description = "Classic adapter can select a nested table as its own model root")
  public void supportsNestedClassicRoot() {
    openEdgeCasesFixture();
    SelenideElement table = $("#nested-classic table");
    ElementsCollection rows = classicRows(table);

    rows.shouldHave(size(1));
    classicCells(rows.first()).get(columnIndex(classicHeaders(table), "Country"))
        .shouldHave(exactText("Nested"));
    classicCells(rows.first()).get(columnIndex(classicHeaders(table), "Company"))
        .shouldHave(exactText("Leak"));
  }

  @Test(description = "Generic ARIA adapter addresses role-based grids and survives root remount")
  public void supportsAriaGridAndRemount() {
    openCustomGridsFixture();
    SelenideElement grid = $("#aria-grid");
    ElementsCollection headers = grid.$$(byRole("columnheader"));
    SelenideElement row = grid.$$(byRole("row")).findBy(text("Alfreds"));

    headers.shouldHave(exactTexts("Country", "Company"));
    row.$$(byRole("gridcell")).get(columnIndex(headers, "Company")).shouldHave(exactText("Alfreds"));
    Selenide.executeJavaScript("window.remountAriaGrid()");
    row.$$(byRole("gridcell")).get(columnIndex(headers, "Country")).shouldHave(exactText("Austria"));
  }

  @Test(description = "Headerless and repeated headers preserve typed lookup failures")
  public void handlesHeaderlessAndRepeatedHeaders() {
    openCustomGridsFixture();
    ElementsCollection headerless = $("#headerless-grid").$$(":scope > .header-row");
    SelenideElement repeated = $("#custom-repeated-grid");

    headerless.shouldHave(empty);
    assertThatThrownBy(() -> columnIndex(headerless, "Country", SHORT_TIMEOUT))
        .isInstanceOf(UIAssertionError.class);
    assertThat(gridHeaders(repeated).shouldHave(size(4)).texts())
        .containsExactly("Country", "Company", "Company", "Company");
    gridRows(repeated).shouldHave(size(3));
  }

  private static int columnIndex(ElementsCollection headers, String header) {
    return columnIndex(headers, header, LOOKUP_TIMEOUT);
  }

  private static int columnIndex(ElementsCollection headers, String header, Duration timeout) {
    return headers.shouldHave(itemWithText(header), timeout).texts().indexOf(header);
  }

  private static ElementsCollection classicHeaders(SelenideElement table) {
    return table.$$(byXpath("./thead/tr/th"));
  }

  private static ElementsCollection classicRows(SelenideElement table) {
    return table.$$(byXpath("./tbody/tr"));
  }

  private static ElementsCollection classicCells(SelenideElement row) {
    return row.$$(byXpath("./td | ./th"));
  }

  private static ElementsCollection gridHeaders(SelenideElement grid) {
    return grid.$$(":scope > .header-row > .cell:not([hidden])");
  }

  private static ElementsCollection gridRows(SelenideElement grid) {
    return grid.$$(":scope > .data-row");
  }

  private static ElementsCollection gridCells(SelenideElement row) {
    return row.$$(GRID_CELLS);
  }
}
