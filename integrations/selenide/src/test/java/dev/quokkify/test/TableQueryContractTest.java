package dev.quokkify.test;

import java.time.Duration;
import java.util.Map;
import java.util.stream.IntStream;

import dev.quokkify.elements.table.classic.FlexTable;
import dev.quokkify.elements.table.classic.SelenideDataTable;
import dev.quokkify.elements.table.classic.Table;
import dev.quokkify.elements.table.model.RowConditions;
import dev.quokkify.elements.table.model.SelenideTableQuery;
import dev.quokkify.elements.table.model.TableColumnAmbiguousException;
import dev.quokkify.elements.table.model.TableColumnNotFoundException;
import dev.quokkify.elements.table.model.TableDomAdapters;
import dev.quokkify.elements.table.model.TableRowAmbiguousException;
import dev.quokkify.elements.table.model.TableRowNotFoundException;
import dev.quokkify.elements.table.model.TypedTableCellRef;
import dev.quokkify.elements.table.model.TypedTableColumnRef;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TableQueryContractTest extends BaseTest {

  private enum Header {
    COUNTRY("Country"),
    COMPANY("Company"),
    EMPLOYEES("Employees"),
    MISSING("Missing");

    private final String displayed;

    Header(String displayed) {
      this.displayed = displayed;
    }
  }

  private FixturePage page;

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
    page = Selenide.page(FixturePage.class);
  }

  @Test(dataProvider = "tableModelContractIterations",
      description = "Zero-based rows, cells, and vertical columns expose lazy references")
  public void addressesClassicTableByIndexAndTypedKey(String iteration) {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);
    TypedTableCellRef<Header> company = query.row(0).requiredCell(Header.COMPANY);

    Assertions.assertThat(query.firstRow().cell(0).text()).isEqualTo("Austria");
    Assertions.assertThat(query.lastRow().requiredCell(Header.COMPANY).text()).isEqualTo("Alpine");
    Assertions.assertThat(query.cell(1, 1).text()).isEqualTo("Berglunds");
    Assertions.assertThat(company.rowIndex()).isZero();
    Assertions.assertThat(company.columnIndex()).isEqualTo(1);
    Assertions.assertThat(query.column(1).cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");
    Assertions.assertThat(query.column(Header.COMPANY).cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");

    Selenide.executeJavaScript("window.remountQueryClassic()");
    Assertions.assertThat(company.text()).isEqualTo("Alfreds");
  }

  @Test(description = "Typed column references resolve their index after remount and header reorder")
  public void reResolvesTypedColumnAfterHeaderReorder() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);
    TypedTableColumnRef<Header> company = query.column(Header.COMPANY);

    Assertions.assertThat(company.index()).isEqualTo(1);
    Selenide.executeJavaScript("window.remountQueryClassicWithReorderedHeaders()");

    Assertions.assertThat(company.index()).isZero();
    Assertions.assertThat(company.cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");
  }

  @Test(description = "Mounted and visible rows have explicit and different semantics")
  public void distinguishesMountedFromVisibleRows() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThat(query.mountedRows()).hasSize(4);
    Assertions.assertThat(query.visibleRows()).hasSize(3);
    Assertions.assertThat(query.mountedRows().get(2).isVisible()).isFalse();
  }

  @Test(description = "Conditions compose and preserve duplicate match DOM order")
  public void composesConditionsAndPreservesOrder() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThat(query.findRows(RowConditions.exact(Header.COUNTRY, "Austria")))
        .extracting(row -> row.requiredCell(Header.COMPANY).text())
        .containsExactly("Alfreds", "Alpine");
    Assertions.assertThat(query.findRow(RowConditions.contains(Header.COMPANY, "glund")))
        .hasValueSatisfying(row -> Assertions.assertThat(row.index()).isEqualTo(1));
    Assertions.assertThat(query.findRows(RowConditions.regex(Header.COMPANY, "Al.*")))
        .hasSize(2);
    Assertions.assertThat(query.findRows(RowConditions.all(
        RowConditions.exact(Header.COUNTRY, "Germany"),
        RowConditions.greaterThan(Header.EMPLOYEES, 15))))
        .hasSize(1);
  }

  @Test(description = "Required queries wait natively and unique queries reject zero or duplicates")
  public void waitsAndEnforcesUniqueRows() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);
    Selenide.executeJavaScript("window.prepareDelayedQueryRow()");

    Assertions.assertThat(query.findRow(RowConditions.exact(Header.COMPANY, "Berglunds"))).isEmpty();
    Selenide.executeJavaScript("window.restoreDelayedQueryRow()");
    Assertions.assertThat(query.requiredRow(RowConditions.exact(Header.COMPANY, "Berglunds"),
        Duration.ofSeconds(2)).requiredCell(Header.COUNTRY).text()).isEqualTo("Germany");
    Assertions.assertThat(query.uniqueRow(RowConditions.exact(Header.COMPANY, "Berglunds"),
        Duration.ofSeconds(2)).requiredCell(Header.COMPANY).text()).isEqualTo("Berglunds");
    Assertions.assertThatThrownBy(() -> query.uniqueRow(
            RowConditions.exact(Header.COUNTRY, "Austria")))
        .isInstanceOf(TableRowAmbiguousException.class)
        .hasMessageContaining("2");
    Assertions.assertThatThrownBy(() -> query.uniqueRow(
            RowConditions.exact(Header.COMPANY, "Absent")))
        .isInstanceOf(TableRowNotFoundException.class);
  }

  @Test(description = "Timed unique lookup preserves the last observed zero, one, or multiple count")
  public void timedUniqueRowsReportObservedCardinality() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThat(query.uniqueRow(RowConditions.exact(Header.COMPANY, "Alfreds"),
        Duration.ofMillis(100)).requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThatThrownBy(() -> query.uniqueRow(
            RowConditions.exact(Header.COMPANY, "Absent"), Duration.ofMillis(100)))
        .isInstanceOf(TableRowNotFoundException.class);
    Assertions.assertThatThrownBy(() -> query.uniqueRow(
            RowConditions.exact(Header.COUNTRY, "Austria"), Duration.ofMillis(100)))
        .isInstanceOf(TableRowAmbiguousException.class)
        .hasMessageContaining("2");
  }

  @Test(description = "Horizontal typed lookup waits for a delayed row and rejects duplicate headers")
  public void horizontalTypedLookupWaitsAndRejectsDuplicates() {
    SelenideTableQuery<Header> query = SelenideTableQuery.of(
        page.horizontal, TableDomAdapters.horizontal(), header -> header.displayed);
    Selenide.executeJavaScript("window.prepareDelayedQueryHorizontalRow()");
    Assertions.assertThat(query.requiredRow(row -> row.cell(Header.COMPANY).isPresent(),
        Duration.ofSeconds(2)).requiredCell(Header.COMPANY).text()).isEqualTo("Alfreds");

    Selenide.executeJavaScript("window.duplicateQueryHorizontalHeader()");
    Assertions.assertThatThrownBy(() -> query.requiredRow(
            row -> row.cell(Header.COMPANY).isPresent(), Duration.ofMillis(200)))
        .isInstanceOf(dev.quokkify.elements.table.model.TableColumnAmbiguousException.class)
        .hasMessageContaining("Company");
  }

  @Test(description = "Timeout lookup evaluates conditions with the actual mounted-row index")
  public void preservesRowIndexInsideNativeWait() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThat(query.requiredRow(row -> row.index() == 1, Duration.ofMillis(200)).index())
        .isEqualTo(1);
  }

  @Test(description = "Native query waits keep cell reads on the same candidate snapshot")
  public void keepsIndexedAndTypedConditionReadsOnCapturedSnapshot() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThat(query.requiredRow(row -> {
      if (row.index() == 1) {
        Selenide.executeJavaScript(
            "const body = document.querySelector('#query-classic tbody');"
                + "body.prepend(body.lastElementChild);");
      }
      return row.requiredCell(Header.COMPANY).text().equals("Berglunds");
    }, Duration.ofSeconds(2)).index()).isEqualTo(1);
  }

  @Test(description = "Flex columns are vertical and horizontal logical columns contain one cell")
  public void appliesLayoutSpecificColumnSemantics() {
    SelenideTableQuery<Header> flex = page.flex.query(header -> header.displayed);
    SelenideTableQuery<Header> horizontal = SelenideTableQuery.of(
        page.horizontal, TableDomAdapters.horizontal(), header -> header.displayed);

    Assertions.assertThat(flex.column(Header.COMPANY).cells()).extracting(cell -> cell.text())
        .containsExactly("Quokkify");
    Assertions.assertThat(horizontal.column(Header.COMPANY).cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds");
    Assertions.assertThat(horizontal.column(1).cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds");
  }

  @Test(description = "Missing keys and out-of-range zero-based indexes fail explicitly")
  public void reportsMissingAndOutOfRangeReferences() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThatThrownBy(() -> query.row(4)).isInstanceOf(IndexOutOfBoundsException.class);
    Assertions.assertThatThrownBy(() -> query.cell(0, 3).text())
        .isInstanceOf(IndexOutOfBoundsException.class);
    Assertions.assertThatThrownBy(() -> query.column(3)).isInstanceOf(IndexOutOfBoundsException.class);
    Assertions.assertThatThrownBy(() -> query.column(Header.MISSING))
        .isInstanceOf(TableColumnNotFoundException.class);
  }

  @Test(description = "String headers require an explicit identity resolver")
  public void supportsStringKeysOnlyWithExplicitResolver() {
    SelenideTableQuery<String> query = SelenideTableQuery.of(
        Selenide.$("#query-classic"), TableDomAdapters.classic(), value -> value);

    Assertions.assertThat(query.column("Company").cells()).extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");
  }

  @Test(description = "String-first data table is injected by Selenide and resolves exact displayed headers")
  public void supportsFindByStringFirstComponent() {
    SelenideDataTable customers = page.customers;

    Assertions.assertThat(SelenideTableQuery.byHeaderText(
        customers.getSelf(), TableDomAdapters.classic()).column("Company").cells())
        .extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");
    Assertions.assertThat(customers.query().column("Company").cells())
        .extracting(cell -> cell.text())
        .containsExactly("Alfreds", "Berglunds", "", "Alpine");
    Assertions.assertThat(customers.query().row(0).requiredCell("Company").text())
        .isEqualTo("Alfreds");
  }

  @Test(description = "String-first table supports map shortcuts and composed native cell assertions")
  public void supportsMapShortcutsAndRowConditions() {
    SelenideDataTable customers = page.customers;

    Assertions.assertThat(customers.requiredRow(Map.of("Company", "Berglunds", "Country", "Germany"))
        .requiredCell("Employees").text()).isEqualTo("20");
    customers.shouldHave(Map.of("Company", "Alfreds", "Country", "Austria"));
    customers.query().requiredRow(RowConditions.exact("Company", "Alfreds"))
        .shouldHave(SelenideDataTable.rowConditions(Map.of(
            "Company", Condition.exactTextCaseSensitive("Alfreds"),
            "Country", Condition.exactTextCaseSensitive("Austria"))));
  }

  @Test(description = "String-first row lookup waits for delayed rendering and survives table remount")
  public void waitsForDelayedRowsAndResolvesRemountedRoot() {
    SelenideDataTable customers = page.customers;
    Selenide.executeJavaScript("window.prepareDelayedQueryRow()");
    Selenide.executeJavaScript("window.restoreDelayedQueryRow()");

    var company = customers.requiredRow(Map.of("Company", "Berglunds"))
        .requiredCell("Company");
    Selenide.executeJavaScript("window.remountQueryClassic()");

    Assertions.assertThat(company.text()).isEqualTo("Berglunds");
  }

  @Test(description = "String-first headers fail deterministically for missing and duplicate displayed names")
  public void reportsMissingDuplicateAndNullHeaders() {
    SelenideDataTable customers = page.customers;

    Assertions.assertThatThrownBy(() -> customers.query().column("Missing"))
        .isInstanceOf(TableColumnNotFoundException.class)
        .hasMessageContaining("Missing")
        .hasMessageContaining("[Country, Company, Employees]");
    Selenide.executeJavaScript("document.querySelector('#query-classic thead th').textContent = 'Company'");
    Assertions.assertThatThrownBy(() -> customers.query().column("Company"))
        .isInstanceOf(TableColumnAmbiguousException.class)
        .hasMessageContaining("Company")
        .hasMessageContaining("[Company, Company, Employees]");
    Assertions.assertThatThrownBy(() -> customers.query().column((String) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("column");
  }

  private static final class FixturePage {
    @FindBy(how = How.ID, using = "query-classic")
    private Table<Header> classic;
    @FindBy(how = How.ID, using = "query-flex")
    private FlexTable<Header> flex;
    @FindBy(how = How.ID, using = "query-horizontal")
    private SelenideElement horizontal;
    @FindBy(how = How.ID, using = "query-classic")
    private SelenideDataTable customers;
  }
}
