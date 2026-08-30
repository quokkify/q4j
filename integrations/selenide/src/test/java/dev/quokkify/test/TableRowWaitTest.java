package dev.quokkify.test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import dev.quokkify.elements.table.classic.Row;
import dev.quokkify.elements.table.horizontal.HorizontalRow;
import dev.quokkify.ex.TableRowException;
import dev.quokkify.page.local.DelayedTablePage;
import dev.quokkify.page.local.LateMountingTablePage;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.UIAssertionError;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TableRowWaitTest extends BaseTest {

  private static final String DELAYED_TABLE_FIXTURE_PATH = "/table/delayed-table.html";
  private static final String LATE_MOUNTING_TABLE_FIXTURE_PATH = "/table/late-mounting-table.html";
  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final Duration SHORT_TIMEOUT = Duration.ofMillis(600);

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
    DelayedTablePage page = openDelayedTablePage();

    page.getTableRow(DelayedTablePage.Header.COMPANY, "Ernst Handel", TIMEOUT)
        .verifyCell(DelayedTablePage.Header.COUNTRY, "Austria");
  }

  @TmsLink("UI_ID_8")
  @Test(description = "Verify 'TABLE' row search fails with row details and a chained cause after the given timeout")
  public void testTableRowSearchFailsAfterTimeout() {
    DelayedTablePage page = openDelayedTablePage();
    Instant start = Instant.now();

    Assertions.assertThatThrownBy(() ->
            page.getTableRow(DelayedTablePage.Header.COMPANY, "Missing Company", TIMEOUT))
        .isInstanceOf(TableRowException.class)
        .hasMessageContaining("Missing Company")
        .hasMessageContaining("Company")
        .hasMessageContaining("5s")
        .hasCauseInstanceOf(UIAssertionError.class)
        .cause()
        .hasMessageContaining("customers")
        .hasMessageContaining("row with 'Missing Company' in 'COMPANY' column");

    Assertions.assertThat(Duration.between(start, Instant.now()))
        .isBetween(TIMEOUT, TIMEOUT.plusSeconds(2));
  }

  @TmsLink("UI_ID_9")
  @Test(description = "Verify 'HORIZONTAL TABLE' row search waits for a row appearing with a delay")
  public void testHorizontalTableRowAppearingWithDelayIsFound() {
    DelayedTablePage page = openDelayedTablePage();

    page.getHorizontalTableRow(DelayedTablePage.HorizontalHeader.TELEPHONE_2, TIMEOUT)
        .verifyRow("555 77 855");
  }

  @TmsLink("UI_ID_10")
  @Test(dataProvider = "tableModelContractIterations",
      description = "Verify 'DYNAMIC HORIZONTAL TABLE' row search waits for a row appearing with a delay "
      + "(regression for the column-index lookup throwing before the header renders)")
  public void testDynamicHorizontalTableRowAppearingWithDelayIsFound(String iteration) {
    DelayedTablePage page = openDelayedTablePage();

    page.getDynamicHorizontalTableRow(DelayedTablePage.DynamicHorizontalHeader.TELEPHONE_2, TIMEOUT)
        .verifyRow("555 77 855");
  }

  @TmsLink("UI_ID_11")
  @Test(description = "Verify row search waits out a table container that mounts into the DOM late, "
      + "not just a table whose rows are appended while the container is already visible")
  public void testTableRowWaitSurvivesLateMountingContainer() {
    LateMountingTablePage page = openLateMountingTablePage();

    page.getLateTableRow(LateMountingTablePage.Header.COMPANY, "Ernst Handel", TIMEOUT)
        .verifyCell(LateMountingTablePage.Header.COUNTRY, "Austria");
  }

  @TmsLink("UI_ID_12")
  @Test(description = "Verify row search waits out a table container that starts with zero <tr> elements "
      + "(regression for the negative-index bug in getAllRowsElements() on an empty table)")
  public void testTableRowWaitSurvivesEmptyContainer() {
    LateMountingTablePage page = openLateMountingTablePage();

    page.getEmptyTableRow(LateMountingTablePage.Header.COMPANY, "Ernst Handel", TIMEOUT)
        .verifyCell(LateMountingTablePage.Header.COUNTRY, "Austria");
  }

  @TmsLink("UI_ID_13")
  @Test(description = "Verify 'TABLE' row search by a map of expected values waits for a delayed row")
  public void testGetRowByMapWaitsForDelayedRow() {
    DelayedTablePage page = openDelayedTablePage();

    page.getTableRow(Map.of(DelayedTablePage.Header.COMPANY, "Ernst Handel",
            DelayedTablePage.Header.COUNTRY, "Austria"), TIMEOUT)
        .verifyCell(DelayedTablePage.Header.CONTACT, "Roland Mendel");
  }

  @TmsLink("UI_ID_14")
  @Test(description = "Verify 'TABLE' row search by pattern waits for a delayed row")
  public void testGetRowByPatternWaitsForDelayedRow() {
    DelayedTablePage page = openDelayedTablePage();

    page.getTableRowByPattern(DelayedTablePage.Header.COMPANY, "Ernst.*", TIMEOUT)
        .verifyCell(DelayedTablePage.Header.COUNTRY, "Austria");
  }

  @TmsLink("UI_ID_15")
  @Test(description = "Verify the default-timeout 'TABLE' getRow(column, value) overload also waits for a delayed row")
  public void testGetRowDefaultTimeoutOverloadWaits() {
    DelayedTablePage page = openDelayedTablePage();

    page.getTableRow(DelayedTablePage.Header.COMPANY, "Ernst Handel")
        .verifyCell(DelayedTablePage.Header.COUNTRY, "Austria");
  }

  @TmsLink("UI_ID_16")
  @Test(description = "Verify the default-timeout 'HORIZONTAL TABLE' getRow(column) overload also waits "
      + "for a delayed row")
  public void testHorizontalGetRowDefaultTimeoutOverloadWaits() {
    DelayedTablePage page = openDelayedTablePage();

    page.getHorizontalTableRow(DelayedTablePage.HorizontalHeader.TELEPHONE_2)
        .verifyRow("555 77 855");
  }

  @TmsLink("UI_ID_17")
  @Test(description = "Verify isRowExist(...) stays a non-waiting check: it must return false quickly "
      + "against a row that only appears after a delay, not after the full row-wait timeout")
  public void testIsRowExistDoesNotWaitFullTimeout() {
    DelayedTablePage page = openDelayedTablePage();
    Instant start = Instant.now();

    boolean rowExists = page.isTableRowExist(DelayedTablePage.Header.COMPANY, "Ernst Handel");

    Assertions.assertThat(rowExists).as("Row should not be visible yet").isFalse();
    Assertions.assertThat(Duration.between(start, Instant.now()))
        .as("isRowExist must not wait for an unmounted table")
        .isLessThan(SHORT_TIMEOUT);
  }

  @TmsLink("UI_ID_18")
  @Test(description = "Verify 'DYNAMIC TABLE' row search uses Selenide waiting for a delayed row")
  public void testDynamicTableRowAppearingWithDelayIsFound() {
    DelayedTablePage page = openDelayedTablePage();

    page.getDynamicTableRow(DelayedTablePage.DynamicHeader.COMPANY, "Ernst Handel", TIMEOUT)
        .verifyCell(DelayedTablePage.DynamicHeader.COUNTRY, "Austria");
  }

  @TmsLink("UI_ID_19")
  @Test(description = "Verify 'FLEX TABLE' row search uses Selenide waiting for a delayed row")
  public void testFlexTableRowAppearingWithDelayIsFound() {
    DelayedTablePage page = openDelayedTablePage();

    page.getFlexTableRow(DelayedTablePage.Header.COMPANY, "Ernst Handel", TIMEOUT)
        .verifyCell(DelayedTablePage.Header.COUNTRY, "Austria");
  }

  @TmsLink("UI_ID_20")
  @Test(description = "Verify 'DYNAMIC TABLE' applies the caller timeout")
  public void testDynamicTableMissingRowUsesCallerTimeout() {
    DelayedTablePage page = openDelayedTablePage();

    assertCallerTimeout(() ->
        page.getDynamicTableRow(DelayedTablePage.DynamicHeader.COMPANY, "Missing Company", SHORT_TIMEOUT));
  }

  @TmsLink("UI_ID_21")
  @Test(description = "Verify 'FLEX TABLE' applies the caller timeout")
  public void testFlexTableMissingRowUsesCallerTimeout() {
    DelayedTablePage page = openDelayedTablePage();

    assertCallerTimeout(() ->
        page.getFlexTableRow(DelayedTablePage.Header.COMPANY, "Missing Company", SHORT_TIMEOUT));
  }

  @TmsLink("UI_ID_22")
  @Test(description = "Verify an unmounted table respects a short caller timeout without a nested visibility wait")
  public void testUnmountedTableUsesCallerTimeout() {
    LateMountingTablePage page = openLateMountingTablePage();

    assertCallerTimeout(() ->
        page.getLateTableRow(LateMountingTablePage.Header.COMPANY, "Missing Company", SHORT_TIMEOUT));
  }

  @TmsLink("UI_ID_23")
  @Test(description = "Verify isRowExist remains non-waiting when the table itself is not mounted")
  public void testIsRowExistDoesNotWaitForUnmountedTable() {
    LateMountingTablePage page = openLateMountingTablePage();
    Instant start = Instant.now();

    Assertions.assertThat(page.isLateTableRowExist(LateMountingTablePage.Header.COMPANY, "Ernst Handel"))
        .isFalse();
    Assertions.assertThat(Duration.between(start, Instant.now())).isLessThan(SHORT_TIMEOUT);
  }

  @Test(description = "Verify a returned TABLE row resolves itself again after the table root is replaced")
  public void testReturnedTableRowSurvivesTableReload() {
    DelayedTablePage page = openDelayedTablePage();
    Row<DelayedTablePage.Header> row =
        page.getTableRow(DelayedTablePage.Header.COMPANY, "Ernst Handel", TIMEOUT);

    page.reloadClassicTable();

    row.verifyCell(DelayedTablePage.Header.COUNTRY, "Austria reloaded");
  }

  @Test(description = "Verify a returned DYNAMIC TABLE row resolves itself again after the table root is replaced")
  public void testReturnedDynamicTableRowSurvivesTableReload() {
    DelayedTablePage page = openDelayedTablePage();
    Row<DelayedTablePage.DynamicHeader> row =
        page.getDynamicTableRow(DelayedTablePage.DynamicHeader.COMPANY, "Ernst Handel", TIMEOUT);

    page.reloadClassicTable();

    row.verifyCell(DelayedTablePage.DynamicHeader.COUNTRY, "Austria reloaded");
  }

  @Test(description = "Verify a returned FLEX TABLE row resolves itself again after the table root is replaced")
  public void testReturnedFlexTableRowSurvivesTableReload() {
    DelayedTablePage page = openDelayedTablePage();
    Row<DelayedTablePage.Header> row =
        page.getFlexTableRow(DelayedTablePage.Header.COMPANY, "Ernst Handel", TIMEOUT);

    page.reloadFlexTable();

    row.verifyCell(DelayedTablePage.Header.COUNTRY, "Austria reloaded");
  }

  @Test(description = "Verify a returned HORIZONTAL TABLE row resolves itself again after the table root is replaced")
  public void testReturnedHorizontalTableRowSurvivesTableReload() {
    DelayedTablePage page = openDelayedTablePage();
    HorizontalRow<DelayedTablePage.HorizontalHeader> row =
        page.getHorizontalTableRow(DelayedTablePage.HorizontalHeader.TELEPHONE_2, TIMEOUT);

    page.reloadHorizontalTable();

    row.verifyRow("555 77 856");
  }

  @Test(description = "Verify a returned DYNAMIC HORIZONTAL row resolves itself after the table root is replaced")
  public void testReturnedDynamicHorizontalTableRowSurvivesTableReload() {
    DelayedTablePage page = openDelayedTablePage();
    HorizontalRow<DelayedTablePage.DynamicHorizontalHeader> row =
        page.getDynamicHorizontalTableRow(DelayedTablePage.DynamicHorizontalHeader.TELEPHONE_2, TIMEOUT);

    page.reloadHorizontalTable();

    row.verifyRow("555 77 856");
  }

  private void assertCallerTimeout(Runnable lookup) {
    Instant start = Instant.now();
    Assertions.assertThatThrownBy(lookup::run)
        .isInstanceOf(TableRowException.class)
        .hasMessageContaining("600ms")
        .hasCauseInstanceOf(UIAssertionError.class);
    Assertions.assertThat(Duration.between(start, Instant.now()))
        .isBetween(SHORT_TIMEOUT, SHORT_TIMEOUT.plusSeconds(2));
  }

  private DelayedTablePage openDelayedTablePage() {
    return Selenide.open(APP_CONFIG.baseUrl() + DELAYED_TABLE_FIXTURE_PATH, DelayedTablePage.class);
  }

  private LateMountingTablePage openLateMountingTablePage() {
    return Selenide.open(APP_CONFIG.baseUrl() + LATE_MOUNTING_TABLE_FIXTURE_PATH, LateMountingTablePage.class);
  }
}
