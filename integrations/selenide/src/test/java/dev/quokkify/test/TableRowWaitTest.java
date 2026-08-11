package dev.quokkify.test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import dev.quokkify.ex.TableRowException;
import dev.quokkify.page.local.DelayedTablePage;
import dev.quokkify.page.local.LateMountingTablePage;

import com.codeborne.selenide.Selenide;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.awaitility.core.ConditionTimeoutException;
import org.testng.annotations.Test;

public class TableRowWaitTest extends BaseTest {

  private static final String DELAYED_TABLE_FIXTURE_PATH = "/table/delayed-table.html";
  private static final String LATE_MOUNTING_TABLE_FIXTURE_PATH = "/table/late-mounting-table.html";
  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final Duration POLLING_INTERVAL = Duration.ofMillis(200);

  @TmsLink("UI_ID_7")
  @Test(description = "Verify 'TABLE' row search waits for a row appearing with a delay")
  public void testTableRowAppearingWithDelayIsFound() {
    DelayedTablePage page = openDelayedTablePage();

    page.getTableRow(DelayedTablePage.Header.COMPANY, "Ernst Handel", TIMEOUT, POLLING_INTERVAL)
        .verifyCell(DelayedTablePage.Header.COUNTRY, "Austria");
  }

  @TmsLink("UI_ID_8")
  @Test(description = "Verify 'TABLE' row search fails with row details and a chained cause after the given timeout")
  public void testTableRowSearchFailsAfterTimeout() {
    DelayedTablePage page = openDelayedTablePage();
    Instant start = Instant.now();

    Assertions.assertThatThrownBy(() ->
            page.getTableRow(DelayedTablePage.Header.COMPANY, "Missing Company", TIMEOUT, POLLING_INTERVAL))
        .isInstanceOf(TableRowException.class)
        .hasMessageContaining("Missing Company")
        .hasMessageContaining("Company")
        .hasCauseInstanceOf(ConditionTimeoutException.class);

    Assertions.assertThat(Duration.between(start, Instant.now()))
        .isBetween(TIMEOUT, TIMEOUT.plusSeconds(2));
  }

  @TmsLink("UI_ID_9")
  @Test(description = "Verify 'HORIZONTAL TABLE' row search waits for a row appearing with a delay")
  public void testHorizontalTableRowAppearingWithDelayIsFound() {
    DelayedTablePage page = openDelayedTablePage();

    page.getHorizontalTableRow(DelayedTablePage.HorizontalHeader.TELEPHONE_2, TIMEOUT, POLLING_INTERVAL)
        .verifyRow("555 77 855");
  }

  @TmsLink("UI_ID_10")
  @Test(description = "Verify 'DYNAMIC HORIZONTAL TABLE' row search waits for a row appearing with a delay "
      + "(regression for the column-index lookup throwing before the header renders)")
  public void testDynamicHorizontalTableRowAppearingWithDelayIsFound() {
    DelayedTablePage page = openDelayedTablePage();

    page.getDynamicHorizontalTableRow(DelayedTablePage.HorizontalHeader.TELEPHONE_2, TIMEOUT, POLLING_INTERVAL)
        .verifyRow("555 77 855");
  }

  @TmsLink("UI_ID_11")
  @Test(description = "Verify row search waits out a table container that mounts into the DOM late, "
      + "not just a table whose rows are appended while the container is already visible")
  public void testTableRowWaitSurvivesLateMountingContainer() {
    LateMountingTablePage page = openLateMountingTablePage();

    page.getLateTableRow(LateMountingTablePage.Header.COMPANY, "Ernst Handel", TIMEOUT, POLLING_INTERVAL)
        .verifyCell(LateMountingTablePage.Header.COUNTRY, "Austria");
  }

  @TmsLink("UI_ID_12")
  @Test(description = "Verify row search waits out a table container that starts with zero <tr> elements "
      + "(regression for the negative-index bug in getAllRowsElements() on an empty table)")
  public void testTableRowWaitSurvivesEmptyContainer() {
    LateMountingTablePage page = openLateMountingTablePage();

    page.getEmptyTableRow(LateMountingTablePage.Header.COMPANY, "Ernst Handel", TIMEOUT, POLLING_INTERVAL)
        .verifyCell(LateMountingTablePage.Header.COUNTRY, "Austria");
  }

  @TmsLink("UI_ID_13")
  @Test(description = "Verify 'TABLE' row search by a map of expected values waits for a delayed row")
  public void testGetRowByMapWaitsForDelayedRow() {
    DelayedTablePage page = openDelayedTablePage();

    page.getTableRow(Map.of(DelayedTablePage.Header.COMPANY, "Ernst Handel",
            DelayedTablePage.Header.COUNTRY, "Austria"), TIMEOUT, POLLING_INTERVAL)
        .verifyCell(DelayedTablePage.Header.CONTACT, "Roland Mendel");
  }

  @TmsLink("UI_ID_14")
  @Test(description = "Verify 'TABLE' row search by pattern waits for a delayed row")
  public void testGetRowByPatternWaitsForDelayedRow() {
    DelayedTablePage page = openDelayedTablePage();

    page.getTableRowByPattern(DelayedTablePage.Header.COMPANY, "Ernst.*", TIMEOUT, POLLING_INTERVAL)
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
        .as("isRowExist() must not wait for the row-wait timeout")
        .isLessThan(TIMEOUT);
  }

  private DelayedTablePage openDelayedTablePage() {
    return Selenide.open(fixtureUrl(DELAYED_TABLE_FIXTURE_PATH), DelayedTablePage.class);
  }

  private LateMountingTablePage openLateMountingTablePage() {
    return Selenide.open(fixtureUrl(LATE_MOUNTING_TABLE_FIXTURE_PATH), LateMountingTablePage.class);
  }

  // classpath: URLs silently failed to navigate in this environment, hence the external-form file: URL below.
  private String fixtureUrl(String fixturePath) {
    return Objects.requireNonNull(getClass().getResource(fixturePath), "Fixture not found: " + fixturePath)
        .toExternalForm();
  }
}
