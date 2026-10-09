package dev.quokkify.test;

import java.time.Duration;

import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableColumnException;
import dev.quokkify.elements.table.TableLayout;
import dev.quokkify.elements.table.TableRow;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.ElementNotFound;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TableTest extends BaseTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  @Test
  public void findsDelayedRowByColumnValue() {
    Table customers = openDelayedCustomers();

    customers.row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
  }

  @Test
  public void matchesValueOnlyInGivenColumn() {
    Table t = openQueryClassic();

    t.rows("Company", "Austria").shouldHave(size(0));
    t.rows("Country", "Austria").shouldHave(size(2));
  }

  @Test
  public void returnsAllMatchingRows() {
    Table t = openQueryClassic();

    t.rows("Country", "Austria").shouldHave(size(2));
    t.row("Country", "Austria").cell("Company").shouldHave(exactText("Alfreds"));
  }

  @Test
  public void readsColumnByHeader() {
    Table customers = openDelayedCustomers();

    customers.column("Company").shouldHave(exactTexts("Alfreds Futterkiste", "Ernst Handel"), TIMEOUT);
  }

  @Test
  public void rowsLookupWaitsWhileHeadersMount() {
    Table t = openQueryClassic();
    Allure.step("Detach thead, re-insert it after 300 ms", () -> Selenide.executeJavaScript(
        "const table = document.getElementById('query-classic');"
            + "const head = table.tHead; head.remove();"
            + "setTimeout(() => table.insertBefore(head, table.tBodies[0]), 300);"));

    t.rows("Company", "Alfreds").shouldHave(size(1), Duration.ofSeconds(2));
  }

  @Test
  public void cellWaitsForLateHeader() {
    Table t = openQueryClassic();
    TableRow berglunds = t.row("Company", "Berglunds");
    Allure.step("Remove header Employees, re-append it after 500 ms", () -> Selenide.executeJavaScript(
        "const tr = document.querySelector('#query-classic thead tr');"
            + "const th = tr.children[2]; th.remove();"
            + "setTimeout(() => tr.appendChild(th), 500);"));

    berglunds.cell("Employees").shouldHave(exactText("20"));
  }

  @Test
  public void wrapsRowFoundByCaller() {
    Table t = openQueryClassic();
    TableRow berglunds = t.row(t.rows().findBy(text("Berglunds")));

    berglunds.cell("Country").shouldHave(exactText("Germany"));
    berglunds.cell("Employees").shouldHave(exactText("20"));
  }

  @Test
  public void missingHeaderThrowsTableColumnException() {
    Table t = openQueryClassic();

    assertThatThrownBy(() -> t.row(0).cell("Region"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("Region")
        .hasMessageContaining("[Country, Company, Employees]");
  }

  @Test
  public void duplicateHeaderInRowLookupThrows() {
    openEdgeCasesFixture();
    Table t = Table.of($("#repeated-table"), TableLayout.html());

    assertThatThrownBy(() -> t.row("Company", "x").self().should(exist, Duration.ofMillis(500)))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("ambiguous");
  }

  @Test
  public void rowLookupWaitsForLateColumn() {
    Table t = openQueryClassic();

    Allure.step("Rename header Employees → Staff, restore after 300 ms", TableTest::renameEmployeesHeaderTemporarily);
    t.row("Employees", "20").cell("Company").shouldHave(exactText("Berglunds"), Duration.ofSeconds(2));
    Allure.step("Rename header Employees → Staff, restore after 300 ms", TableTest::renameEmployeesHeaderTemporarily);
    t.rows("Employees", "20").shouldHave(size(1), Duration.ofSeconds(2));
  }

  @Test
  public void missingColumnInRowLookupFailsWithElementNotFound() {
    Table t = openQueryClassic();

    assertThatThrownBy(() -> t.row("Region", "x").self().should(exist, Duration.ofMillis(500)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Region");
  }

  @Test
  public void missingRowFailsWithElementNotFound() {
    Table customers = openDelayedCustomers();

    assertThatThrownBy(() -> customers.row("Company", "Missing Company").self().should(exist, Duration.ofMillis(600)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Company = \"Missing Company\"");
  }

  @Test
  public void capturedRowSurvivesRemount() {
    Table customers = openDelayedCustomers();
    TableRow row = customers.row("Company", "Ernst Handel");

    row.cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
    Allure.step("Remount classic table with Country → Austria reloaded",
        () -> Selenide.executeJavaScript("window.reloadClassicTable()"));
    row.cell("Country").shouldHave(exactText("Austria reloaded"), TIMEOUT);
  }

  @Test
  public void rowLookupSurvivesHeaderReorder() {
    Table t = openQueryClassic();
    TableRow row = t.row("Company", "Berglunds");

    row.self().shouldHave(text("Germany"));
    Allure.step("Remount query table with reordered headers",
        () -> Selenide.executeJavaScript("window.remountQueryClassicWithReorderedHeaders()"));
    row.cell("Country").shouldHave(exactText("Germany"));
  }

  @Test
  public void htmlLayoutIgnoresNestedTableRows() {
    openEdgeCasesFixture();
    Table t = Table.of($("#nested-classic"), TableLayout.html());

    t.rows().shouldHave(size(1));
  }

  @Test
  public void waitsForLateMountedRoot() {
    Selenide.open(APP_CONFIG.baseUrl() + "/table/late-mounting-table.html");
    Table late = Table.of($("#late-customers"), TableLayout.of(
        By.xpath("./tbody/tr[td]"), By.xpath("./td"), By.xpath("./tbody/tr[th]/th")));

    late.row("Company", "Ernst Handel").self().shouldBe(visible, TIMEOUT);
  }

  @Test
  public void readsAriaGrid() {
    openCustomGridsFixture();
    Table t = Table.of($("#aria-grid"), TableLayout.aria());

    t.headers().shouldHave(exactTexts("Country", "Company"));
    t.row("Country", "Austria").cell("Company").shouldHave(exactText("Alfreds"));
  }

  @Test
  public void readsCustomDivGridWithHiddenHeaderCell() {
    openCustomGridsFixture();
    Table grid = Table.of($("#custom-grid"), TableLayout.of(By.cssSelector(":scope > .data-row"),
        By.cssSelector(":scope > .cell"), By.cssSelector(":scope > .header-row > .cell")));

    grid.rows().shouldHave(size(3));
    grid.row("Country", "Austria").cell("Company").shouldHave(text("Outer"));
    grid.rows("Company", "anything").shouldHave(size(0));
  }

  @Test
  public void readsFlexTable() {
    Table flex = openFlexCustomers();

    flex.row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
  }

  @Test
  public void columnRequiresXpathLayout() {
    Table flex = openFlexCustomers();

    assertThatThrownBy(() -> flex.column("Company")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void multiRowTheadKeepsColumnsAligned() {
    Table t = tableFromMarkup("Inject table with grouped two-row thead", "<table id='t'><thead>"
        + "<tr><th colspan='2'>Customer</th><th>Location</th></tr>"
        + "<tr><th>Company</th><th>Contact</th><th>Country</th></tr>"
        + "</thead><tbody><tr><td>Ernst</td><td>Roland</td><td>Austria</td></tr></tbody></table>");

    t.row(0).cell("Company").shouldHave(exactText("Ernst"), TIMEOUT);
    t.row(0).cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
  }

  @Test
  public void theadWithTdCells() {
    Table t = tableFromMarkup("Inject table whose thead uses td cells", "<table id='t'><thead>"
        + "<tr><td>Company</td><td>Country</td></tr></thead>"
        + "<tbody><tr><td>Ernst</td><td>Austria</td></tr></tbody></table>");

    t.row("Company", "Ernst").cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
  }

  @Test
  public void rowsLookupWithDuplicateHeaderIsAmbiguous() {
    Table t = tableFromMarkup("Inject table with two Company headers", "<table id='t'><thead>"
        + "<tr><th>Company</th><th>Company</th></tr></thead>"
        + "<tbody><tr><td>A</td><td>B</td></tr></tbody></table>");

    assertThatThrownBy(() -> t.rows("Company", "A").shouldHave(size(1), Duration.ofMillis(500)))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("ambiguous");
  }

  @Test
  public void valueWithQuotes() {
    Table t = tableFromMarkup("Inject table with a name containing quotes", "<table id='t'><thead>"
        + "<tr><th>Name</th><th>Status</th></tr></thead>"
        + "<tbody><tr><td>O'Brien \"Jr\"</td><td>Ready</td></tr></tbody></table>");

    t.row("Name", "O'Brien \"Jr\"").cell("Status").shouldHave(exactText("Ready"), TIMEOUT);
  }

  @Test
  public void headerWithSortIconIsNotMatchedByLabel() {
    Table t = tableFromMarkup("Inject table with a sort icon inside the Company header", "<table id='t'><thead>"
        + "<tr><th>Company <span class='sort'>▲</span></th><th>Country</th></tr></thead>"
        + "<tbody><tr><td>Ernst</td><td>Austria</td></tr></tbody></table>");

    assertThatThrownBy(() -> t.row(0).cell("Company"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("Company ▲");
  }

  @Test
  public void inputCellValueViaCallerFoundRow() {
    Table t = tableFromMarkup("Inject table whose Name cell holds an input", "<table id='t'><thead>"
        + "<tr><th>Name</th><th>Status</th></tr></thead>"
        + "<tbody><tr><td><input value='Alpha'></td><td>Ready</td></tr></tbody></table>");

    t.row($("#t input[value='Alpha']").closest("tr")).cell("Status").shouldHave(exactText("Ready"), TIMEOUT);
  }

  private static Table openDelayedCustomers() {
    Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html");
    return Table.of($("#customers"), TableLayout.html());
  }

  private static Table openQueryClassic() {
    openQueriesFixture();
    return Table.of($("#query-classic"), TableLayout.html());
  }

  private static Table tableFromMarkup(String step, String html) {
    Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html");
    Allure.step(step, () -> Selenide.executeJavaScript("document.body.innerHTML = arguments[0]", html));
    return Table.of($("#t"), TableLayout.html());
  }

  private static void renameEmployeesHeaderTemporarily() {
    Selenide.executeJavaScript("const th = document.querySelector('#query-classic thead tr').children[2];"
        + "th.textContent = 'Staff';"
        + "setTimeout(() => { th.textContent = 'Employees'; }, 300);");
  }

  private static Table openFlexCustomers() {
    Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html");
    return Table.of($("#flex-customers"), TableLayout.of(
        By.cssSelector(":scope > .flex-table-row:not(:first-child)"), By.cssSelector(":scope > div"),
        By.cssSelector(":scope > .flex-table-row:first-child > div")));
  }
}
