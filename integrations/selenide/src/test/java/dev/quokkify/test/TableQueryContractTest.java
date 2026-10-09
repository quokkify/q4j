package dev.quokkify.test;

import java.time.Duration;
import java.util.stream.IntStream;

import dev.quokkify.elements.table.HorizontalTable;
import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableColumnException;
import dev.quokkify.elements.table.TableLayout;
import dev.quokkify.elements.table.TableRow;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.UIAssertionError;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exactTextCaseSensitive;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class TableQueryContractTest extends BaseTest {

  private static final int COMPANY = 1;

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
    Table table = queryClassic();
    SelenideElement company = table.row(0).cell("Company");

    table.row(0).cell(0).shouldHave(exactText("Austria"));
    table.row(3).cell("Company").shouldHave(exactText("Alpine"));
    table.row(1).cell(1).shouldHave(exactText("Berglunds"));
    company.shouldHave(exactText("Alfreds"));
    table.column("Company").shouldHave(exactTexts("Alfreds", "Berglunds", "Hidden Co", "Alpine"));

    Selenide.executeJavaScript("window.remountQueryClassic()");
    company.shouldHave(exactText("Alfreds"));
  }

  @Test(description = "Typed column references resolve their index after remount and header reorder")
  public void reResolvesTypedColumnAfterHeaderReorder() {
    Table table = queryClassic();

    table.row(0).cell(1).shouldHave(exactText("Alfreds"));
    Selenide.executeJavaScript("window.remountQueryClassicWithReorderedHeaders()");

    table.row(0).cell(0).shouldHave(exactText("Alfreds"));
    table.column("Company").shouldHave(exactTexts("Alfreds", "Berglunds", "Hidden Co", "Alpine"));
  }

  @Test(description = "Mounted and visible rows have explicit and different semantics")
  public void distinguishesMountedFromVisibleRows() {
    ElementsCollection rows = queryClassic().rows();

    rows.shouldHave(size(4));
    rows.filterBy(visible).shouldHave(size(3));
    rows.get(2).shouldBe(hidden);
  }

  @Test(description = "Conditions compose and preserve duplicate match DOM order")
  public void composesConditionsAndPreservesOrder() {
    Table table = queryClassic();

    ElementsCollection austria = table.rows("Country", "Austria");
    austria.shouldHave(size(2));
    austria.get(0).shouldHave(text("Alfreds"));
    austria.get(1).shouldHave(text("Alpine"));
    table.rows("Company", "Berglunds").shouldHave(size(1));
    table.row(1).self().shouldHave(text("glund"));
    austria.filterBy(text("Al")).shouldHave(size(2));
    table.rows("Country", "Germany").filterBy(text("20")).shouldHave(size(1));
  }

  @Test(description = "Required queries wait natively and unique queries reject zero or duplicates")
  public void waitsAndEnforcesUniqueRows() {
    Table table = queryClassic();
    Selenide.executeJavaScript("window.prepareDelayedQueryRow()");

    table.rows("Company", "Berglunds").shouldHave(size(0));
    Selenide.executeJavaScript("window.restoreDelayedQueryRow()");
    table.root().shouldHave(text("Berglunds"), Duration.ofSeconds(2));
    table.rows("Company", "Berglunds").shouldHave(size(1), Duration.ofSeconds(2));
    table.row("Company", "Berglunds").cell("Country").shouldHave(exactText("Germany"), Duration.ofSeconds(2));
    table.row("Company", "Berglunds").cell("Company").shouldHave(exactText("Berglunds"), Duration.ofSeconds(2));
    Assertions.assertThatThrownBy(() -> table.rows("Country", "Austria")
            .shouldHave(size(1), Duration.ofMillis(100)))
        .isInstanceOf(UIAssertionError.class)
        .hasMessageContaining("2");
    Assertions.assertThatThrownBy(() -> table.row("Company", "Absent").self()
            .should(exist, Duration.ofMillis(100)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Absent");
  }

  @Test(description = "Timed unique lookup preserves the last observed zero, one, or multiple count")
  public void timedUniqueRowsReportObservedCardinality() {
    Table table = queryClassic();

    table.rows("Company", "Alfreds").shouldHave(size(1), Duration.ofMillis(100));
    table.row("Company", "Alfreds").cell("Country").shouldHave(exactText("Austria"), Duration.ofMillis(100));
    Assertions.assertThatThrownBy(() -> table.row("Company", "Absent").self()
            .should(exist, Duration.ofMillis(100)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Absent");
    Assertions.assertThatThrownBy(() -> table.rows("Country", "Austria")
            .shouldHave(size(1), Duration.ofMillis(100)))
        .isInstanceOf(UIAssertionError.class)
        .hasMessageContaining("2");
  }

  @Test(description = "Horizontal typed lookup waits for a delayed row and rejects duplicate headers")
  public void horizontalTypedLookupWaitsAndRejectsDuplicates() {
    HorizontalTable horizontal = HorizontalTable.of($("#query-horizontal"));
    Selenide.executeJavaScript("window.prepareDelayedQueryHorizontalRow()");
    horizontal.value("Company").shouldHave(exactText("Alfreds"), Duration.ofSeconds(2));

    Selenide.executeJavaScript("window.duplicateQueryHorizontalHeader()");
    Assertions.assertThatThrownBy(() -> horizontal.headers().filterBy(exactText("Company"))
            .shouldHave(size(1), Duration.ofMillis(200)))
        .isInstanceOf(UIAssertionError.class)
        .hasMessageContaining("Company");
  }

  @Test(description = "Timeout lookup evaluates conditions with the actual mounted-row index")
  public void preservesRowIndexInsideNativeWait() {
    queryClassic().row(1).self().shouldHave(text("Berglunds"), Duration.ofMillis(200));
  }

  @Test(description = "Native query waits keep cell reads on the same candidate snapshot")
  public void keepsIndexedAndTypedConditionReadsOnCapturedSnapshot() {
    WebElement captured = queryClassic().row(1).self().toWebElement();

    Selenide.executeJavaScript(
        "const body = document.querySelector('#query-classic tbody');"
            + "body.prepend(body.lastElementChild);");

    Assertions.assertThat(captured.findElements(By.xpath("./td")).get(COMPANY).getText())
        .isEqualTo("Berglunds");
  }

  @Test(description = "Flex columns are vertical and horizontal logical columns contain one cell")
  public void appliesLayoutSpecificColumnSemantics() {
    Table flex = Table.of($("#query-flex"), TableLayout.of(
        By.cssSelector(":scope > .flex-table-row:not(:first-child)"), By.cssSelector(":scope > div"),
        By.cssSelector(":scope > .flex-table-row:first-child > div")));
    SelenideElement horizontal = $("#query-horizontal");

    flex.rows().shouldHave(size(1));
    flex.row(0).cell("Company").shouldHave(exactText("Quokkify"));
    HorizontalTable.of(horizontal).value("Company").shouldHave(exactText("Alfreds"));
    horizontal.$$("tr").get(1).$$("td").shouldHave(exactTexts("Alfreds"));
  }

  @Test(description = "Missing keys and out-of-range zero-based indexes fail explicitly")
  public void reportsMissingAndOutOfRangeReferences() {
    Table table = queryClassic();

    Assertions.assertThatThrownBy(() -> table.row(4).self().should(exist, Duration.ofMillis(100)))
        .isInstanceOf(ElementNotFound.class);
    Assertions.assertThatThrownBy(() -> table.row(0).cell(3).should(exist, Duration.ofMillis(100)))
        .isInstanceOf(ElementNotFound.class);
    Assertions.assertThatThrownBy(() -> table.headers().get(3).should(exist, Duration.ofMillis(100)))
        .isInstanceOf(ElementNotFound.class);
    Assertions.assertThatThrownBy(() -> table.row(0).cell("Missing"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("Missing");
  }

  @Test(description = "String headers require an explicit identity resolver")
  public void supportsStringKeysOnlyWithExplicitResolver() {
    queryClassic().column("Company").shouldHave(exactTexts("Alfreds", "Berglunds", "Hidden Co", "Alpine"));
  }

  @Test(description = "String-first data table is injected by Selenide and resolves exact displayed headers")
  public void supportsFindByStringFirstComponent() {
    Table customers = queryClassic();

    customers.column("Company").shouldHave(exactTexts("Alfreds", "Berglunds", "Hidden Co", "Alpine"));
    customers.row(0).cell("Company").shouldHave(exactText("Alfreds"));
  }

  @Test(description = "String-first table supports map shortcuts and composed native cell assertions")
  public void supportsMapShortcutsAndRowConditions() {
    Table customers = queryClassic();

    customers.rows("Company", "Berglunds").filterBy(text("Germany")).shouldHave(size(1));
    customers.row("Company", "Berglunds").cell("Employees").shouldHave(exactText("20"));
    customers.rows("Company", "Alfreds").filterBy(text("Austria")).shouldHave(size(1));
    TableRow alfreds = customers.row("Company", "Alfreds");
    alfreds.cell("Company").shouldHave(exactTextCaseSensitive("Alfreds"));
    alfreds.cell("Country").shouldHave(exactTextCaseSensitive("Austria"));
  }

  @Test(description = "String-first row lookup waits for delayed rendering and survives table remount")
  public void waitsForDelayedRowsAndResolvesRemountedRoot() {
    Table customers = queryClassic();
    Selenide.executeJavaScript("window.prepareDelayedQueryRow()");
    Selenide.executeJavaScript("window.restoreDelayedQueryRow()");

    SelenideElement company = customers.row("Company", "Berglunds").cell("Company");
    company.shouldHave(exactText("Berglunds"), Duration.ofSeconds(2));
    Selenide.executeJavaScript("window.remountQueryClassic()");

    company.shouldHave(exactText("Berglunds"), Duration.ofSeconds(2));
  }

  @Test(description = "String-first headers fail deterministically for missing and duplicate displayed names")
  public void reportsMissingDuplicateAndNullHeaders() {
    Table customers = queryClassic();

    Assertions.assertThatThrownBy(() -> customers.row(0).cell("Missing"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("Missing");
    Selenide.executeJavaScript("document.querySelector('#query-classic thead th').textContent = 'Company'");
    Assertions.assertThatThrownBy(() -> customers.row(0).cell("Company"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("Company")
        .hasMessageContaining("ambiguous");
  }

  private static Table queryClassic() {
    return Table.of($("#query-classic"), TableLayout.html());
  }
}
