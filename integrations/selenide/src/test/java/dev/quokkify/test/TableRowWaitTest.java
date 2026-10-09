package dev.quokkify.test;

import java.time.Duration;
import java.time.Instant;

import dev.quokkify.elements.table.HorizontalTable;
import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableLayout;
import dev.quokkify.elements.table.TableRow;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.By;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.textCaseSensitive;
import static com.codeborne.selenide.Selenide.$;

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

    customers().row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_8")
  @Test(description = "Verify 'TABLE' row search fails with row details and a chained cause after the given timeout")
  public void testTableRowSearchFailsAfterTimeout() {
    openDelayedTablePage();
    Instant start = Instant.now();

    Assertions.assertThatThrownBy(() -> customers().row("Company", "Missing Company").self()
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

    horizontalCustomers().value("Telephone 2").shouldHave(exactText("555 77 855"), TIMEOUT);
  }

  @TmsLink("UI_ID_10")
  @Test(dataProvider = "tableModelContractIterations",
      description = "Verify 'DYNAMIC HORIZONTAL TABLE' row search waits for a row appearing with a delay "
      + "(regression for the column-index lookup throwing before the header renders)")
  public void testDynamicHorizontalTableRowAppearingWithDelayIsFound(String iteration) {
    openDelayedTablePage();

    horizontalCustomers().value("Telephone 2").shouldHave(exactText("555 77 855"), TIMEOUT);
  }

  @TmsLink("UI_ID_11")
  @Test(description = "Verify row search waits out a table container that mounts into the DOM late, "
      + "not just a table whose rows are appended while the container is already visible")
  public void testTableRowWaitSurvivesLateMountingContainer() {
    openLateMountingTablePage();

    lateCustomers("#late-customers").row("Company", "Ernst Handel").cell("Country")
        .shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_12")
  @Test(description = "Verify row search waits out a table container that starts with zero <tr> elements "
      + "(regression for the negative-index bug in getAllRowsElements() on an empty table)")
  public void testTableRowWaitSurvivesEmptyContainer() {
    openLateMountingTablePage();

    lateCustomers("#empty-customers").row("Company", "Ernst Handel").cell("Country")
        .shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_13")
  @Test(description = "Verify 'TABLE' row search by a map of expected values waits for a delayed row")
  public void testGetRowByMapWaitsForDelayedRow() {
    openDelayedTablePage();
    Table customers = customers();

    customers.rows("Company", "Ernst Handel").filterBy(text("Austria")).shouldHave(size(1), TIMEOUT);
    customers.row("Company", "Ernst Handel").cell("Contact").shouldHave(exactText("Roland Mendel"), TIMEOUT);
  }

  @TmsLink("UI_ID_14")
  @Test(description = "Verify 'TABLE' row search by pattern waits for a delayed row")
  public void testGetRowByPatternWaitsForDelayedRow() {
    openDelayedTablePage();
    Table customers = customers();

    customers.column("Company").findBy(textCaseSensitive("Ernst")).shouldBe(exist, TIMEOUT);
    customers.row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_15")
  @Test(description = "Verify the default-timeout 'TABLE' getRow(column, value) overload also waits for a delayed row")
  public void testGetRowDefaultTimeoutOverloadWaits() {
    openDelayedTablePage();

    customers().row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"));
  }

  @TmsLink("UI_ID_16")
  @Test(description = "Verify the default-timeout 'HORIZONTAL TABLE' getRow(column) overload also waits "
      + "for a delayed row")
  public void testHorizontalGetRowDefaultTimeoutOverloadWaits() {
    openDelayedTablePage();

    horizontalCustomers().value("Telephone 2").shouldHave(exactText("555 77 855"));
  }

  @TmsLink("UI_ID_17")
  @Test(description = "Verify isRowExist(...) stays a non-waiting check: it must return false quickly "
      + "against a row that only appears after a delay, not after the full row-wait timeout")
  public void testIsRowExistDoesNotWaitFullTimeout() {
    openDelayedTablePage();
    Instant start = Instant.now();

    boolean rowExists = customers().row("Company", "Ernst Handel").self().exists();

    Assertions.assertThat(rowExists).as("Row should not be visible yet").isFalse();
    Assertions.assertThat(Duration.between(start, Instant.now()))
        .as("isRowExist must not wait for an unmounted table")
        .isLessThan(NON_WAITING_TIMEOUT);
  }

  @TmsLink("UI_ID_18")
  @Test(description = "Verify 'DYNAMIC TABLE' row search uses Selenide waiting for a delayed row")
  public void testDynamicTableRowAppearingWithDelayIsFound() {
    openDelayedTablePage();

    customers().column("Company").findBy(exactText("Ernst Handel")).shouldBe(exist, TIMEOUT);
    customers().row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_19")
  @Test(description = "Verify 'FLEX TABLE' row search uses Selenide waiting for a delayed row")
  public void testFlexTableRowAppearingWithDelayIsFound() {
    openDelayedTablePage();

    flexCustomers().row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
  }

  @TmsLink("UI_ID_20")
  @Test(description = "Verify 'DYNAMIC TABLE' applies the caller timeout")
  public void testDynamicTableMissingRowUsesCallerTimeout() {
    openDelayedTablePage();

    assertCallerTimeout(() -> customers().row("Company", "Missing Company").self().should(exist, SHORT_TIMEOUT));
  }

  @TmsLink("UI_ID_21")
  @Test(description = "Verify 'FLEX TABLE' applies the caller timeout")
  public void testFlexTableMissingRowUsesCallerTimeout() {
    openDelayedTablePage();

    assertCallerTimeout(() -> flexCustomers().row("Company", "Missing Company").self()
        .should(exist, SHORT_TIMEOUT));
  }

  @TmsLink("UI_ID_22")
  @Test(description = "Verify an unmounted table respects a short caller timeout without a nested visibility wait")
  public void testUnmountedTableUsesCallerTimeout() {
    openLateMountingTablePage();

    assertCallerTimeout(() -> lateCustomers("#late-customers").row("Company", "Missing Company").self()
        .should(exist, SHORT_TIMEOUT));
  }

  @TmsLink("UI_ID_23")
  @Test(description = "Verify isRowExist remains non-waiting when the table itself is not mounted")
  public void testIsRowExistDoesNotWaitForUnmountedTable() {
    openLateMountingTablePage();
    Instant start = Instant.now();

    Assertions.assertThat(lateCustomers("#late-customers").row("Company", "Ernst Handel").self().exists())
        .isFalse();
    Assertions.assertThat(Duration.between(start, Instant.now())).isLessThan(NON_WAITING_TIMEOUT);
  }

  @Test(description = "Verify a returned TABLE row resolves itself again after the table root is replaced")
  public void testReturnedTableRowSurvivesTableReload() {
    openDelayedTablePage();
    TableRow row = customers().row("Company", "Ernst Handel");
    row.self().shouldBe(exist, TIMEOUT);

    Selenide.executeJavaScript("window.reloadClassicTable()");

    row.cell("Country").shouldHave(exactText("Austria reloaded"), TIMEOUT);
  }

  @Test(description = "Verify a returned DYNAMIC TABLE row resolves itself again after the table root is replaced")
  public void testReturnedDynamicTableRowSurvivesTableReload() {
    openDelayedTablePage();
    TableRow row = customers().row("Company", "Ernst Handel");
    row.self().shouldBe(exist, TIMEOUT);

    Selenide.executeJavaScript("window.reloadClassicTable()");

    row.cell("Country").shouldHave(exactText("Austria reloaded"), TIMEOUT);
  }

  @Test(description = "Verify a returned FLEX TABLE row resolves itself again after the table root is replaced")
  public void testReturnedFlexTableRowSurvivesTableReload() {
    openDelayedTablePage();
    TableRow row = flexCustomers().row("Company", "Ernst Handel");
    row.self().shouldBe(exist, TIMEOUT);

    Selenide.executeJavaScript("window.reloadFlexTable()");

    row.cell(2).shouldHave(exactText("Austria reloaded"), TIMEOUT);
  }

  @Test(description = "Verify a returned HORIZONTAL TABLE row resolves itself again after the table root is replaced")
  public void testReturnedHorizontalTableRowSurvivesTableReload() {
    openDelayedTablePage();
    SelenideElement value = horizontalCustomers().value("Telephone 2");
    value.shouldHave(exactText("555 77 855"), TIMEOUT);

    Selenide.executeJavaScript("window.reloadHorizontalTable()");

    value.shouldHave(exactText("555 77 856"), TIMEOUT);
  }

  @Test(description = "Verify a returned DYNAMIC HORIZONTAL row resolves itself after the table root is replaced")
  public void testReturnedDynamicHorizontalTableRowSurvivesTableReload() {
    openDelayedTablePage();
    SelenideElement value = horizontalCustomers().value("Telephone 2");
    value.shouldHave(exactText("555 77 855"), TIMEOUT);

    Selenide.executeJavaScript("window.reloadHorizontalTable()");

    value.shouldHave(exactText("555 77 856"), TIMEOUT);
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

  private static Table customers() {
    return Table.of($("#customers"), TableLayout.html());
  }

  private static Table flexCustomers() {
    return Table.of($("#flex-customers"), TableLayout.of(
        By.cssSelector(":scope > .flex-table-row:not(:first-child)"), By.cssSelector(":scope > div"),
        By.cssSelector(":scope > .flex-table-row:first-child > div")));
  }

  private static Table lateCustomers(String selector) {
    return Table.of($(selector), TableLayout.of(
        By.xpath("./tbody/tr[td]"), By.xpath("./td"), By.xpath("./tbody/tr[th]/th")));
  }

  private static HorizontalTable horizontalCustomers() {
    return HorizontalTable.of($("#horizontal-customers"));
  }

  private void openDelayedTablePage() {
    Selenide.open(APP_CONFIG.baseUrl() + DELAYED_TABLE_FIXTURE_PATH);
  }

  private void openLateMountingTablePage() {
    Selenide.open(APP_CONFIG.baseUrl() + LATE_MOUNTING_TABLE_FIXTURE_PATH);
  }
}
