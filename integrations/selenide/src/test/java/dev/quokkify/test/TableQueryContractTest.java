package dev.quokkify.test;

import java.time.Duration;

import dev.quokkify.elements.table.classic.FlexTable;
import dev.quokkify.elements.table.classic.Table;
import dev.quokkify.elements.table.model.RowConditions;
import dev.quokkify.elements.table.model.SelenideTableQuery;
import dev.quokkify.elements.table.model.TableColumnNotFoundException;
import dev.quokkify.elements.table.model.TableDomAdapters;
import dev.quokkify.elements.table.model.TableQueryRow;
import dev.quokkify.elements.table.model.TableRowAmbiguousException;
import dev.quokkify.elements.table.model.TableRowNotFoundException;
import dev.quokkify.elements.table.model.TypedTableCellRef;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;
import org.testng.annotations.BeforeMethod;
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

  @BeforeMethod
  public void openFixture() {
    String baseUrl = System.getenv().getOrDefault("NGINX_BASE_URL", "http://localhost");
    Selenide.open(baseUrl + "/table-model-contract/");
    page = Selenide.page(FixturePage.class);
  }

  @Test(description = "Zero-based rows, cells, and vertical columns expose lazy references")
  public void addressesClassicTableByIndexAndTypedKey() {
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
    TableQueryRow<Header> restored = query.requiredRow(
        RowConditions.exact(Header.COMPANY, "Berglunds"), Duration.ofSeconds(2));
    Assertions.assertThat(restored.index()).isEqualTo(1);
    Assertions.assertThat(restored.requiredCell(Header.COUNTRY).text()).isEqualTo("Germany");
    Assertions.assertThatThrownBy(() -> query.uniqueRow(
            RowConditions.exact(Header.COUNTRY, "Austria")))
        .isInstanceOf(TableRowAmbiguousException.class)
        .hasMessageContaining("2");
    Assertions.assertThatThrownBy(() -> query.uniqueRow(
            RowConditions.exact(Header.COMPANY, "Absent")))
        .isInstanceOf(TableRowNotFoundException.class);
  }

  @Test(description = "Timeout lookup evaluates conditions with the actual mounted-row index")
  public void preservesRowIndexInsideNativeWait() {
    SelenideTableQuery<Header> query = page.classic.query(header -> header.displayed);

    Assertions.assertThat(query.requiredRow(row -> row.index() == 1, Duration.ofMillis(200)).index())
        .isEqualTo(1);
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
    Assertions.assertThatThrownBy(() -> query.cell(0, 3)).isInstanceOf(IndexOutOfBoundsException.class);
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

  private static final class FixturePage {
    @FindBy(how = How.ID, using = "query-classic")
    private Table<Header> classic;
    @FindBy(how = How.ID, using = "query-flex")
    private FlexTable<Header> flex;
    @FindBy(how = How.ID, using = "query-horizontal")
    private SelenideElement horizontal;
  }
}
