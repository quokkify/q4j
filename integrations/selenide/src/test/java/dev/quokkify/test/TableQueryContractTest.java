package dev.quokkify.test;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.UIAssertionError;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byRole;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class TableQueryContractTest extends BaseTest {

  private static final int COUNTRY = 0;
  private static final int COMPANY = 1;
  private static final int EMPLOYEES = 2;

  @DataProvider(name = "tableModelContractIterations", parallel = false)
  public Object[][] tableModelContractIterations() {
    int repetitions = Integer.parseInt(System.getProperty("tableModel.contract.repetitions", "1"));
    return IntStream.rangeClosed(1, repetitions)
        .mapToObj(iteration -> new Object[] {"iteration-%02d".formatted(iteration)})
        .toArray(Object[][]::new);
  }

  @BeforeMethod
  public void openFixture() {
    openQueriesFixture();
  }

  @Test(dataProvider = "tableModelContractIterations",
      description = "Zero-based rows, cells, and vertical columns expose lazy references")
  public void addressesClassicTableByIndexAndTypedKey(String iteration) {
    SelenideElement table = $("#query-classic");
    ElementsCollection rows = rows(table);
    int companyColumn = columnIndex(table, "Company");
    SelenideElement company = cells(rows.get(0)).get(companyColumn);

    cells(rows.first()).get(0).shouldHave(exactText("Austria"));
    cells(rows.last()).get(companyColumn).shouldHave(exactText("Alpine"));
    cells(rows.get(1)).get(1).shouldHave(exactText("Berglunds"));
    Assertions.assertThat(companyColumn).isEqualTo(1);
    table.$$(":scope > tbody > tr > td:nth-child(2)")
        .shouldHave(exactTexts("Alfreds", "Berglunds", "Hidden Co", "Alpine"));
    table.$$(":scope > tbody > tr > td:nth-child(" + (companyColumn + 1) + ")")
        .shouldHave(exactTexts("Alfreds", "Berglunds", "Hidden Co", "Alpine"));

    Selenide.executeJavaScript("window.remountQueryClassic()");
    company.shouldHave(exactText("Alfreds"));
  }

  @Test(description = "Typed column references resolve their index after remount and header reorder")
  public void reResolvesTypedColumnAfterHeaderReorder() {
    SelenideElement table = $("#query-classic");

    Assertions.assertThat(columnIndex(table, "Company")).isEqualTo(1);
    Selenide.executeJavaScript("window.remountQueryClassicWithReorderedHeaders()");

    int company = columnIndex(table, "Company");
    Assertions.assertThat(company).isZero();
    table.$$(":scope > tbody > tr > td:nth-child(" + (company + 1) + ")")
        .shouldHave(exactTexts("Alfreds", "Berglunds", "Hidden Co", "Alpine"));
  }

  @Test(description = "Mounted and visible rows have explicit and different semantics")
  public void distinguishesMountedFromVisibleRows() {
    ElementsCollection rows = rows($("#query-classic"));

    rows.shouldHave(size(4));
    rows.filterBy(visible).shouldHave(size(3));
    rows.get(2).shouldBe(hidden);
  }

  @Test(description = "Conditions compose and preserve duplicate match DOM order")
  public void composesConditionsAndPreservesOrder() {
    ElementsCollection rows = rows($("#query-classic"));

    ElementsCollection austria = rows.filterBy(cell(COUNTRY, "Country equals Austria", "Austria"::equals));
    austria.shouldHave(size(2));
    cells(austria.get(0)).get(COMPANY).shouldHave(exactText("Alfreds"));
    cells(austria.get(1)).get(COMPANY).shouldHave(exactText("Alpine"));
    rows.filterBy(cell(COMPANY, "Company contains glund", value -> value.contains("glund")))
        .shouldHave(size(1));
    rows.get(1).shouldHave(cell(COMPANY, "Company contains glund", value -> value.contains("glund")));
    rows.filterBy(cell(COMPANY, "Company matches Al.*", value -> value.matches("Al.*")))
        .shouldHave(size(2));
    rows.filterBy(Condition.and("Germany with more than 15 employees",
            cell(COUNTRY, "Country equals Germany", "Germany"::equals),
            cell(EMPLOYEES, "Employees greater than 15", value -> Integer.parseInt(value) > 15)))
        .shouldHave(size(1));
  }

  @Test(description = "Required queries wait natively and unique queries reject zero or duplicates")
  public void waitsAndEnforcesUniqueRows() {
    ElementsCollection rows = rows($("#query-classic"));
    WebElementCondition berglunds = cell(COMPANY, "Company equals Berglunds", "Berglunds"::equals);
    Selenide.executeJavaScript("window.prepareDelayedQueryRow()");

    rows.filterBy(berglunds).shouldHave(size(0));
    Selenide.executeJavaScript("window.restoreDelayedQueryRow()");
    $("#query-classic").shouldHave(text("Berglunds"), Duration.ofSeconds(2));
    rows.filterBy(berglunds).shouldHave(size(1), Duration.ofSeconds(2));
    cells(rows.findBy(berglunds)).get(COUNTRY).shouldHave(exactText("Germany"), Duration.ofSeconds(2));
    cells(rows.findBy(berglunds)).get(COMPANY).shouldHave(exactText("Berglunds"), Duration.ofSeconds(2));
    Assertions.assertThatThrownBy(() -> rows
            .filterBy(cell(COUNTRY, "Country equals Austria", "Austria"::equals))
            .shouldHave(size(1), Duration.ofMillis(100)))
        .isInstanceOf(UIAssertionError.class)
        .hasMessageContaining("2");
    Assertions.assertThatThrownBy(() -> rows
            .findBy(cell(COMPANY, "Company equals Absent", "Absent"::equals))
            .should(exist, Duration.ofMillis(100)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Absent");
  }

  @Test(description = "Timed unique lookup preserves the last observed zero, one, or multiple count")
  public void timedUniqueRowsReportObservedCardinality() {
    ElementsCollection rows = rows($("#query-classic"));

    rows.filterBy(cell(COMPANY, "Company equals Alfreds", "Alfreds"::equals))
        .shouldHave(size(1), Duration.ofMillis(100));
    cells(rows.findBy(cell(COMPANY, "Company equals Alfreds", "Alfreds"::equals)))
        .get(COUNTRY).shouldHave(exactText("Austria"), Duration.ofMillis(100));
    Assertions.assertThatThrownBy(() -> rows
            .findBy(cell(COMPANY, "Company equals Absent", "Absent"::equals))
            .should(exist, Duration.ofMillis(100)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Absent");
    Assertions.assertThatThrownBy(() -> rows
            .filterBy(cell(COUNTRY, "Country equals Austria", "Austria"::equals))
            .shouldHave(size(1), Duration.ofMillis(100)))
        .isInstanceOf(UIAssertionError.class)
        .hasMessageContaining("2");
  }

  @Test(description = "Horizontal typed lookup waits for a delayed row and rejects duplicate headers")
  public void horizontalTypedLookupWaitsAndRejectsDuplicates() {
    SelenideElement horizontal = $("#query-horizontal");
    Selenide.executeJavaScript("window.prepareDelayedQueryHorizontalRow()");
    horizontal.$$("tr").findBy(text("Company")).$("td")
        .shouldHave(exactText("Alfreds"), Duration.ofSeconds(2));

    Selenide.executeJavaScript("window.duplicateQueryHorizontalHeader()");
    Assertions.assertThatThrownBy(() -> horizontal.$$("th").filterBy(exactText("Company"))
            .shouldHave(size(1), Duration.ofMillis(200)))
        .isInstanceOf(UIAssertionError.class)
        .hasMessageContaining("Company");
  }

  @Test(description = "Timeout lookup evaluates conditions with the actual mounted-row index")
  public void preservesRowIndexInsideNativeWait() {
    rows($("#query-classic")).get(1).shouldHave(text("Berglunds"), Duration.ofMillis(200));
  }

  @Test(description = "Native query waits keep cell reads on the same candidate snapshot")
  public void keepsIndexedAndTypedConditionReadsOnCapturedSnapshot() {
    WebElement captured = rows($("#query-classic")).get(1).toWebElement();

    Selenide.executeJavaScript(
        "const body = document.querySelector('#query-classic tbody');"
            + "body.prepend(body.lastElementChild);");

    Assertions.assertThat(captured.findElements(By.xpath("./td")).get(COMPANY).getText())
        .isEqualTo("Berglunds");
  }

  @Test(description = "Flex columns are vertical and horizontal logical columns contain one cell")
  public void appliesLayoutSpecificColumnSemantics() {
    ElementsCollection flexRows = $$("#query-flex > .flex-table-row");
    int flexCompany = flexRows.first().$$(":scope > div").texts().indexOf("Company");
    SelenideElement horizontal = $("#query-horizontal");

    $$("#query-flex > .flex-table-row:not(:first-child) > div:nth-child(" + (flexCompany + 1) + ")")
        .shouldHave(exactTexts("Quokkify"));
    horizontal.$$("tr").findBy(text("Company")).$$("td").shouldHave(exactTexts("Alfreds"));
    horizontal.$$("tr").get(1).$$("td").shouldHave(exactTexts("Alfreds"));
  }

  @Test(description = "Missing keys and out-of-range zero-based indexes fail explicitly")
  public void reportsMissingAndOutOfRangeReferences() {
    SelenideElement table = $("#query-classic");
    ElementsCollection rows = rows(table);

    Assertions.assertThatThrownBy(() -> rows.get(4).should(exist, Duration.ofMillis(100)))
        .isInstanceOf(ElementNotFound.class);
    Assertions.assertThatThrownBy(() -> cells(rows.get(0)).get(3).should(exist, Duration.ofMillis(100)))
        .isInstanceOf(ElementNotFound.class);
    Assertions.assertThatThrownBy(() -> table.$$(":scope > thead th").get(3)
            .should(exist, Duration.ofMillis(100)))
        .isInstanceOf(ElementNotFound.class);
    Assertions.assertThatThrownBy(() -> table.$$(byRole("columnheader"))
            .shouldHave(itemWithText("Missing"), Duration.ofMillis(100)))
        .isInstanceOf(UIAssertionError.class)
        .hasMessageContaining("Missing");
  }

  @Test(description = "String headers require an explicit identity resolver")
  public void supportsStringKeysOnlyWithExplicitResolver() {
    SelenideElement table = $("#query-classic");

    table.$$(":scope > tbody > tr > td:nth-child(" + (columnIndex(table, "Company") + 1) + ")")
        .shouldHave(exactTexts("Alfreds", "Berglunds", "Hidden Co", "Alpine"));
  }

  @Test(description = "String-first data table is injected by Selenide and resolves exact displayed headers")
  public void supportsFindByStringFirstComponent() {
    SelenideElement customers = $("#query-classic");
    int company = columnIndex(customers, "Company");

    customers.$$(":scope > tbody > tr > td:nth-child(" + (company + 1) + ")")
        .shouldHave(exactTexts("Alfreds", "Berglunds", "Hidden Co", "Alpine"));
    cells(rows(customers).get(0)).get(company).shouldHave(exactText("Alfreds"));
  }

  @Test(description = "String-first table supports map shortcuts and composed native cell assertions")
  public void supportsMapShortcutsAndRowConditions() {
    SelenideElement customers = $("#query-classic");
    ElementsCollection rows = rows(customers);
    int country = columnIndex(customers, "Country");
    int company = columnIndex(customers, "Company");
    int employees = columnIndex(customers, "Employees");

    ElementsCollection berglunds = rows
        .filterBy(cell(company, "Company equals Berglunds", "Berglunds"::equals))
        .filterBy(cell(country, "Country equals Germany", "Germany"::equals));
    berglunds.shouldHave(size(1));
    cells(berglunds.first()).get(employees).shouldHave(exactText("20"));
    rows.findBy(Condition.and("Company Alfreds in Austria",
            cell(company, "Company equals Alfreds", "Alfreds"::equals),
            cell(country, "Country equals Austria", "Austria"::equals)))
        .should(exist);
    SelenideElement alfreds = rows.findBy(cell(company, "Company equals Alfreds", "Alfreds"::equals));
    cells(alfreds).get(company).shouldHave(exactTextCaseSensitive("Alfreds"));
    cells(alfreds).get(country).shouldHave(exactTextCaseSensitive("Austria"));
  }

  @Test(description = "String-first row lookup waits for delayed rendering and survives table remount")
  public void waitsForDelayedRowsAndResolvesRemountedRoot() {
    SelenideElement customers = $("#query-classic");
    Selenide.executeJavaScript("window.prepareDelayedQueryRow()");
    Selenide.executeJavaScript("window.restoreDelayedQueryRow()");

    SelenideElement company = cells(rows(customers)
        .findBy(cell(COMPANY, "Company equals Berglunds", "Berglunds"::equals)))
        .get(columnIndex(customers, "Company"));
    company.shouldHave(exactText("Berglunds"), Duration.ofSeconds(2));
    Selenide.executeJavaScript("window.remountQueryClassic()");

    company.shouldHave(exactText("Berglunds"), Duration.ofSeconds(2));
  }

  @Test(description = "String-first headers fail deterministically for missing and duplicate displayed names")
  public void reportsMissingDuplicateAndNullHeaders() {
    SelenideElement customers = $("#query-classic");

    Assertions.assertThatThrownBy(() -> customers.$$(byRole("columnheader"))
            .shouldHave(itemWithText("Missing"), Duration.ofMillis(100)))
        .isInstanceOf(UIAssertionError.class)
        .hasMessageContaining("Missing");
    Selenide.executeJavaScript("document.querySelector('#query-classic thead th').textContent = 'Company'");
    Assertions.assertThatThrownBy(() -> customers.$$(byRole("columnheader"))
            .filterBy(exactText("Company")).shouldHave(size(1), Duration.ofMillis(100)))
        .isInstanceOf(UIAssertionError.class)
        .hasMessageContaining("Company");
  }

  private static ElementsCollection rows(SelenideElement table) {
    return table.$$(":scope > tbody > tr");
  }

  private static ElementsCollection cells(SelenideElement row) {
    return row.$$(":scope > td");
  }

  private static int columnIndex(SelenideElement table, String header) {
    ElementsCollection headers = table.$$(byRole("columnheader")).shouldHave(itemWithText(header));
    return headers.texts().indexOf(header);
  }

  private static WebElementCondition cell(int column, String description, Predicate<String> predicate) {
    return Condition.match(description,
        row -> predicate.test(row.findElements(By.xpath("./td")).get(column).getText()));
  }
}
