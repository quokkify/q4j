package dev.quokkify.test;

import java.time.Duration;
import java.time.Instant;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class TableRowWaitTest extends BaseTest {

  private static final String DELAYED_TABLE_FIXTURE_PATH = "/table/delayed-table.html";
  private static final String LATE_MOUNTING_TABLE_FIXTURE_PATH = "/table/late-mounting-table.html";
  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final Duration SHORT_TIMEOUT = Duration.ofMillis(600);
  private static final Duration NON_WAITING_TIMEOUT = Duration.ofSeconds(1);

  @DataProvider(name = "tableModelContractIterations", parallel = false)
  public Object[][] tableModelContractIterations() {
    int repetitions = Integer.parseInt(System.getProperty("tableModel.contract.repetitions", "1"));
    return java.util.stream.IntStream.rangeClosed(1, repetitions)
        .mapToObj(iteration -> new Object[] {"iteration-%02d".formatted(iteration)})
        .toArray(Object[][]::new);
  }

  @TmsLink("UI_ID_7")
  @Test(description = "Verify 'TABLE' row search waits for a row appearing with a delay")
  public void testTableRowAppearingWithDelayIsFound() {
    openDelayedTablePage();
    SelenideElement table = $("#customers");

    table.$$("tbody tr").findBy(text("Ernst Handel"))
        .$$("td").get(columnIndex(table, "Country"))
        .shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_8")
  @Test(description = "Verify 'TABLE' row search fails with row details and a chained cause after the given timeout")
  public void testTableRowSearchFailsAfterTimeout() {
    openDelayedTablePage();
    Instant start = Instant.now();

    Assertions.assertThatThrownBy(() -> $("#customers").$$("tbody tr").findBy(text("Missing Company"))
            .should(exist, TIMEOUT))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Missing Company")
        .hasMessageContaining("customers")
        .hasMessageContaining("5s");

    Assertions.assertThat(Duration.between(start, Instant.now()))
        .isBetween(TIMEOUT, TIMEOUT.plusSeconds(2));
  }

  @TmsLink("UI_ID_9")
  @Test(description = "Verify 'HORIZONTAL TABLE' row search waits for a row appearing with a delay")
  public void testHorizontalTableRowAppearingWithDelayIsFound() {
    openDelayedTablePage();

    $$("#horizontal-customers tr").findBy(text("Telephone 2")).$("td")
        .shouldHave(exactText("555 77 855"), TIMEOUT);
  }

  @TmsLink("UI_ID_10")
  @Test(dataProvider = "tableModelContractIterations",
      description = "Verify 'DYNAMIC HORIZONTAL TABLE' row search waits for a row appearing with a delay "
      + "(regression for the column-index lookup throwing before the header renders)")
  public void testDynamicHorizontalTableRowAppearingWithDelayIsFound(String iteration) {
    openDelayedTablePage();

    $$("#horizontal-customers tr").findBy(text("Telephone 2")).$("td")
        .shouldHave(exactText("555 77 855"), TIMEOUT);
  }

  @TmsLink("UI_ID_11")
  @Test(description = "Verify row search waits out a table container that mounts into the DOM late, "
      + "not just a table whose rows are appended while the container is already visible")
  public void testTableRowWaitSurvivesLateMountingContainer() {
    openLateMountingTablePage();
    SelenideElement table = $("#late-customers");

    table.$$("tr").findBy(text("Ernst Handel"))
        .$$("td").get(columnIndex(table, "Country"))
        .shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_12")
  @Test(description = "Verify row search waits out a table container that starts with zero <tr> elements "
      + "(regression for the negative-index bug in getAllRowsElements() on an empty table)")
  public void testTableRowWaitSurvivesEmptyContainer() {
    openLateMountingTablePage();
    SelenideElement table = $("#empty-customers");

    table.$$("tr").findBy(text("Ernst Handel"))
        .$$("td").get(columnIndex(table, "Country"))
        .shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_13")
  @Test(description = "Verify 'TABLE' row search by a map of expected values waits for a delayed row")
  public void testGetRowByMapWaitsForDelayedRow() {
    openDelayedTablePage();
    SelenideElement table = $("#customers");

    table.$$("tbody tr")
        .filterBy(text("Ernst Handel"))
        .filterBy(text("Austria"))
        .first()
        .$$("td").get(columnIndex(table, "Contact"))
        .shouldHave(exactText("Roland Mendel"), TIMEOUT);
  }

  @TmsLink("UI_ID_14")
  @Test(description = "Verify 'TABLE' row search by pattern waits for a delayed row")
  public void testGetRowByPatternWaitsForDelayedRow() {
    openDelayedTablePage();
    SelenideElement table = $("#customers");

    table.$$("tbody tr > td:nth-child(" + (columnIndex(table, "Company") + 1) + ")")
        .findBy(matchText("Ernst.*"))
        .closest("tr")
        .$$("td").get(columnIndex(table, "Country"))
        .shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_15")
  @Test(description = "Verify the default-timeout 'TABLE' getRow(column, value) overload also waits for a delayed row")
  public void testGetRowDefaultTimeoutOverloadWaits() {
    openDelayedTablePage();
    SelenideElement table = $("#customers");

    table.$$("tbody tr").findBy(text("Ernst Handel"))
        .$$("td").get(columnIndex(table, "Country"))
        .shouldHave(exactText("Austria"));
  }

  @TmsLink("UI_ID_16")
  @Test(description = "Verify the default-timeout 'HORIZONTAL TABLE' getRow(column) overload also waits "
      + "for a delayed row")
  public void testHorizontalGetRowDefaultTimeoutOverloadWaits() {
    openDelayedTablePage();

    $$("#horizontal-customers tr").findBy(text("Telephone 2")).$("td")
        .shouldHave(exactText("555 77 855"));
  }

  @TmsLink("UI_ID_17")
  @Test(description = "Verify isRowExist(...) stays a non-waiting check: it must return false quickly "
      + "against a row that only appears after a delay, not after the full row-wait timeout")
  public void testIsRowExistDoesNotWaitFullTimeout() {
    openDelayedTablePage();
    Instant start = Instant.now();

    boolean rowExists = $("#customers").$$("tbody tr").findBy(text("Ernst Handel")).exists();

    Assertions.assertThat(rowExists).as("Row should not be visible yet").isFalse();
    Assertions.assertThat(Duration.between(start, Instant.now()))
        .as("isRowExist must not wait for an unmounted table")
        .isLessThan(NON_WAITING_TIMEOUT);
  }

  @TmsLink("UI_ID_18")
  @Test(description = "Verify 'DYNAMIC TABLE' row search uses Selenide waiting for a delayed row")
  public void testDynamicTableRowAppearingWithDelayIsFound() {
    openDelayedTablePage();
    SelenideElement table = $("#customers");

    table.$$("tbody tr > td:nth-child(" + (columnIndex(table, "Company") + 1) + ")")
        .findBy(exactText("Ernst Handel"))
        .closest("tr")
        .$$("td").get(columnIndex(table, "Country"))
        .shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_19")
  @Test(description = "Verify 'FLEX TABLE' row search uses Selenide waiting for a delayed row")
  public void testFlexTableRowAppearingWithDelayIsFound() {
    openDelayedTablePage();
    SelenideElement table = $("#flex-customers");
    int countryColumn = table.$(":scope > .flex-table-row").$$(":scope > div")
        .shouldHave(itemWithText("Country")).texts().indexOf("Country");

    table.$$(":scope > .flex-table-row:not(:first-child)").findBy(text("Ernst Handel"))
        .$$(":scope > div").get(countryColumn)
        .shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_20")
  @Test(description = "Verify 'DYNAMIC TABLE' applies the caller timeout")
  public void testDynamicTableMissingRowUsesCallerTimeout() {
    openDelayedTablePage();

    assertCallerTimeout(() -> $("#customers").$$("tbody tr > td:nth-child(1)")
        .findBy(exactText("Missing Company")).should(exist, SHORT_TIMEOUT));
  }

  @TmsLink("UI_ID_21")
  @Test(description = "Verify 'FLEX TABLE' applies the caller timeout")
  public void testFlexTableMissingRowUsesCallerTimeout() {
    openDelayedTablePage();

    assertCallerTimeout(() -> $("#flex-customers").$$(":scope > .flex-table-row:not(:first-child)")
        .findBy(text("Missing Company")).should(exist, SHORT_TIMEOUT));
  }

  @TmsLink("UI_ID_22")
  @Test(description = "Verify an unmounted table respects a short caller timeout without a nested visibility wait")
  public void testUnmountedTableUsesCallerTimeout() {
    openLateMountingTablePage();

    assertCallerTimeout(() -> $("#late-customers").$$("tr")
        .findBy(text("Missing Company")).should(exist, SHORT_TIMEOUT));
  }

  @TmsLink("UI_ID_23")
  @Test(description = "Verify isRowExist remains non-waiting when the table itself is not mounted")
  public void testIsRowExistDoesNotWaitForUnmountedTable() {
    openLateMountingTablePage();
    Instant start = Instant.now();

    Assertions.assertThat($("#late-customers").$$("tr").findBy(text("Ernst Handel")).exists())
        .isFalse();
    Assertions.assertThat(Duration.between(start, Instant.now())).isLessThan(NON_WAITING_TIMEOUT);
  }

  @Test(description = "Verify a returned TABLE row resolves itself again after the table root is replaced")
  public void testReturnedTableRowSurvivesTableReload() {
    openDelayedTablePage();
    SelenideElement table = $("#customers");
    SelenideElement row = table.$$("tbody tr").findBy(text("Ernst Handel"));
    row.shouldBe(exist, TIMEOUT);
    int countryColumn = columnIndex(table, "Country");

    Selenide.executeJavaScript("window.reloadClassicTable()");

    row.$$("td").get(countryColumn).shouldHave(exactText("Austria reloaded"), TIMEOUT);
  }

  @Test(description = "Verify a returned DYNAMIC TABLE row resolves itself again after the table root is replaced")
  public void testReturnedDynamicTableRowSurvivesTableReload() {
    openDelayedTablePage();
    SelenideElement table = $("#customers");
    int companyColumn = columnIndex(table, "Company");
    int countryColumn = columnIndex(table, "Country");
    SelenideElement row = table.$$("tbody tr > td:nth-child(" + (companyColumn + 1) + ")")
        .findBy(exactText("Ernst Handel")).closest("tr");
    row.shouldBe(exist, TIMEOUT);

    Selenide.executeJavaScript("window.reloadClassicTable()");

    row.$$("td").get(countryColumn).shouldHave(exactText("Austria reloaded"), TIMEOUT);
  }

  @Test(description = "Verify a returned FLEX TABLE row resolves itself again after the table root is replaced")
  public void testReturnedFlexTableRowSurvivesTableReload() {
    openDelayedTablePage();
    SelenideElement row = $("#flex-customers").$$(":scope > .flex-table-row:not(:first-child)")
        .findBy(text("Ernst Handel"));
    row.shouldBe(exist, TIMEOUT);

    Selenide.executeJavaScript("window.reloadFlexTable()");

    row.$$(":scope > div").get(2).shouldHave(exactText("Austria reloaded"), TIMEOUT);
  }

  @Test(description = "Verify a returned HORIZONTAL TABLE row resolves itself again after the table root is replaced")
  public void testReturnedHorizontalTableRowSurvivesTableReload() {
    openDelayedTablePage();
    SelenideElement row = $$("#horizontal-customers tr").findBy(text("Telephone 2"));
    row.$("td").shouldHave(exactText("555 77 855"), TIMEOUT);

    Selenide.executeJavaScript("window.reloadHorizontalTable()");

    row.$("td").shouldHave(exactText("555 77 856"), TIMEOUT);
  }

  @Test(description = "Verify a returned DYNAMIC HORIZONTAL row resolves itself after the table root is replaced")
  public void testReturnedDynamicHorizontalTableRowSurvivesTableReload() {
    openDelayedTablePage();
    SelenideElement row = $$("#horizontal-customers tr").findBy(text("Telephone 2"));
    row.$("td").shouldHave(exactText("555 77 855"), TIMEOUT);

    Selenide.executeJavaScript("window.reloadHorizontalTable()");

    row.$("td").shouldHave(exactText("555 77 856"), TIMEOUT);
  }

  private void assertCallerTimeout(Runnable lookup) {
    Instant start = Instant.now();
    Assertions.assertThatThrownBy(lookup::run)
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Missing Company")
        .hasMessageContaining("600ms");
    Assertions.assertThat(Duration.between(start, Instant.now()))
        .isBetween(SHORT_TIMEOUT, SHORT_TIMEOUT.plusSeconds(2));
  }

  private static int columnIndex(SelenideElement table, String header) {
    return table.$$("th").shouldHave(itemWithText(header)).texts().indexOf(header);
  }

  private void openDelayedTablePage() {
    Selenide.open(APP_CONFIG.baseUrl() + DELAYED_TABLE_FIXTURE_PATH);
  }

  private void openLateMountingTablePage() {
    Selenide.open(APP_CONFIG.baseUrl() + LATE_MOUNTING_TABLE_FIXTURE_PATH);
  }
}
