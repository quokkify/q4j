package dev.quokkify.test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.elements.table.classic.DynamicTable;
import dev.quokkify.elements.table.classic.FlexTable;
import dev.quokkify.elements.table.classic.Table;
import dev.quokkify.elements.table.horizontal.DynamicHorizontalTable;
import dev.quokkify.elements.table.horizontal.HorizontalTable;
import dev.quokkify.elements.table.model.DisplayedHeaderResolver;
import dev.quokkify.elements.table.model.NoTableHeaders;
import dev.quokkify.elements.table.model.SelenideDomTableModel;
import dev.quokkify.elements.table.model.SelenideTableQuery;
import dev.quokkify.elements.table.model.TableCell;
import dev.quokkify.elements.table.model.TableCellNotFoundException;
import dev.quokkify.elements.table.model.TableColumnAmbiguousException;
import dev.quokkify.elements.table.model.TableColumnNotFoundException;
import dev.quokkify.elements.table.model.TableDomAdapter;
import dev.quokkify.elements.table.model.TableDomAdapters;
import dev.quokkify.elements.table.model.TableHeaderRowLocator;
import dev.quokkify.elements.table.model.TableModel;
import dev.quokkify.elements.table.model.TableRow;
import dev.quokkify.model.ConstantFormat;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TableModelContractTest extends BaseTest {

  private static final String REPETITIONS_PROPERTY = "tableModel.contract.repetitions";

  private enum Header {
    COMPANY("Company"),
    COUNTRY("Country");

    private final String displayed;

    Header(String displayed) {
      this.displayed = displayed;
    }
  }

  @Test(description = "Resolve typed columns by displayed headers rather than enum ordinal")
  public void resolvesDisplayedHeader() {
    TableModel<Header> model = model(List.of("Country", "Company"));

    Assertions.assertThat(model.columnIndex(Header.COMPANY, DisplayedHeaderResolver.requiringNonNull(h -> h.displayed)))
        .isEqualTo(1);
  }

  @Test(description = "Missing typed columns fail with the requested and available displayed headers")
  public void reportsMissingDisplayedHeader() {
    TableModel<Header> model = model(List.of("Country"));

    Assertions.assertThatThrownBy(() -> model.columnIndex(Header.COMPANY,
            DisplayedHeaderResolver.requiringNonNull(h -> h.displayed)))
        .isInstanceOf(TableColumnNotFoundException.class)
        .hasMessageContaining("Company")
        .hasMessageContaining("Country");
  }

  @Test(description = "Reject typed columns whose displayed header occurs more than once")
  public void rejectsAmbiguousDisplayedHeader() {
    TableModel<Header> model = model(List.of("Country", "Company", "Company"));

    Assertions.assertThatThrownBy(() -> model.columnIndex(Header.COMPANY,
            DisplayedHeaderResolver.requiringNonNull(h -> h.displayed)))
        .isInstanceOf(TableColumnAmbiguousException.class)
        .hasMessageContaining("Company")
        .hasMessageContaining("[Country, Company, Company]");
  }

  @Test(description = "Rows expose typed lazy-cell contract without table capabilities")
  public void readsTypedCellLazily() {
    TableRow<Header> row = new TableRow<>() {
      @Override
      public Optional<? extends TableCell<Header>> cell(Header column) {
        return Optional.of(new TableCell<>() {
          @Override
          public Header column() {
            return column;
          }

          @Override
          public String text() {
            return "Austria";
          }
        });
      }
    };

    Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test(description = "The structural table contract supports a backend without browser types")
  public void supportsFullyCustomBackendContract() {
    TableModel<Header> backend = new TableModel<>() {
      private final List<BackendRow> dataRows = List.of(
          new BackendRow(Map.of(Header.COUNTRY, "Austria", Header.COMPANY, "Outer (nested: Leak)")));

      @Override
      public List<String> displayedHeaders() {
        return List.of("Country", "Company");
      }

      @Override
      public List<BackendRow> rows() {
        return dataRows;
      }
    };

    TableRow<Header> row = backend.requiredRow(candidate -> candidate
        .requiredCell(Header.COMPANY).text().startsWith("Outer"), "custom backend row");

    Assertions.assertThat(backend.rows()).hasSize(1);
    Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(row.requiredCell(Header.COMPANY).text())
        .isEqualTo("Outer (nested: Leak)");
    Assertions.assertThat(row.cell(Header.COMPANY)).isPresent();
  }

  @Test(description = "Every legacy table variant exposes the neutral model and typed cells")
  public void bridgesAllLegacyVariants() {
    openFixture();
    FixturePage page = Selenide.page(FixturePage.class);

    Assertions.assertThat(page.classic.asDomModel(h -> h.displayed).rows().get(0)
        .requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(page.dynamic.asDomModel(h -> h.formatValue()).displayedHeaders())
        .containsExactly("Company", "Country");
    Assertions.assertThat(page.flex.asDomModel(h -> h.displayed).displayedHeaders())
        .containsExactly("Country", "Company");
    Assertions.assertThat(page.flex.asDomModel(h -> h.displayed).rows().get(0)
        .requiredCell(Header.COMPANY).text()).isEqualTo("Alfreds");
    Assertions.assertThat(page.horizontal.asDomModel(h -> h.formatValue()).row(
        row -> row.cell(HorizontalHeader.COUNTRY).isPresent()).orElseThrow()
        .requiredCell(HorizontalHeader.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(page.dynamicHorizontal.asDomModel(h -> h.formatValue()).row(
        row -> row.cell(DynamicHorizontalHeader.COUNTRY).isPresent()).orElseThrow()
        .requiredCell(DynamicHorizontalHeader.COUNTRY).text()).isEqualTo("Austria");
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
    openFixture();
    FixturePage page = Selenide.page(FixturePage.class);
    TableModel<Header> model = page.classic.asDomModel(h -> h.displayed);
    Selenide.executeJavaScript("window.prepareDelayedRow()");
    Selenide.$$("#classic tbody tr").shouldHave(CollectionCondition.empty);
    Selenide.executeJavaScript("window.restoreDelayedRow()");
    TableRow<Header> row = model.requiredRow(candidate -> candidate.cell(Header.COMPANY)
        .map(cell -> cell.text().equals("Alfreds")).orElse(false), "company", Duration.ofSeconds(2));
    Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test(dataProvider = "tableModelContractIterations",
      description = "A row reference resolves again after a deterministic DOM remount")
  @SingleThread
  public void rowReferenceSurvivesRemount(String iteration) {
    openFixture();
    FixturePage page = Selenide.page(FixturePage.class);
    TableModel<Header> model = page.classic.asDomModel(h -> h.displayed);
    TableRow<Header> row = model.requiredRow(candidate -> candidate.cell(Header.COMPANY)
        .map(cell -> cell.text().equals("Alfreds")).orElse(false), "company", Duration.ofSeconds(2));
    Selenide.executeJavaScript("window.remount()");
    SelenideLogger.step("Verify remounted row country", () ->
        Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria"));
  }

  @Test(description = "Required row handles skip CLASSIC and FLEX header rows")
  public void requiredRowsSkipHeaders() {
    openFixture();
    FixturePage page = Selenide.page(FixturePage.class);

    TableRow<Header> classicRow = page.classic.asDomModel(h -> h.displayed)
        .requiredRow(candidate -> true, "first classic", Duration.ofMillis(100));
    TableRow<Header> flexRow = page.flex.asDomModel(h -> h.displayed)
        .requiredRow(candidate -> true, "first flex", Duration.ofMillis(100));

    Assertions.assertThat(classicRow.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(flexRow.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Selenide.executeJavaScript("window.remount()");
    Assertions.assertThat(classicRow.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(flexRow.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test(description = "Optional and required lookups distinguish missing rows and cells")
  public void reportsMissingRowsAndCellsConsistently() {
    openFixture();
    FixturePage page = Selenide.page(FixturePage.class);
    TableModel<Header> model = page.classic.asDomModel(h -> h.displayed);
    TableRow<Header> row = model.rows().get(0);

    Assertions.assertThat(row.cell(Header.COUNTRY)).isPresent();
    Selenide.executeJavaScript("window.prepareMissingCell()");
    Assertions.assertThat(row.cell(Header.COMPANY)).isEmpty();
    Assertions.assertThatThrownBy(() -> row.requiredCell(Header.COMPANY))
        .isInstanceOf(TableCellNotFoundException.class)
        .hasMessageContaining("COMPANY");
    Assertions.assertThatThrownBy(() -> model.requiredRow(candidate -> false, "missing", Duration.ofMillis(100)))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("missing");
  }

  @Test(description = "Required lookup waits for a table root mounted after the initial DOM")
  public void waitsForLateRootMount() {
    openFixture();
    FixturePage page = Selenide.page(FixturePage.class);
    TableModel<Header> model = page.classic.asDomModel(h -> h.displayed);

    Selenide.executeJavaScript("window.prepareLateMount()");
    Assertions.assertThat(model.row(candidate -> candidate.cell(Header.COMPANY)
        .map(cell -> cell.text().equals("Alfreds")).orElse(false))).isEmpty();
    Assertions.assertThat(model.requiredRow(candidate -> candidate.cell(Header.COMPANY)
        .map(cell -> cell.text().equals("Alfreds")).orElse(false), "late company", Duration.ofSeconds(2))
        .requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test(description = "Required lookup applies one timeout across late root and row discovery")
  public void timesOutAcrossLateRootAndRowDiscovery() {
    openFixture();
    FixturePage page = Selenide.page(FixturePage.class);
    TableModel<Header> model = page.classic.asDomModel(h -> h.displayed);

    Selenide.executeJavaScript("""
        const table = document.getElementById('classic');
        const row = table.querySelector('tbody tr');
        row.remove();
        table.remove();
        window.setTimeout(() => document.body.prepend(table), 250);
        window.setTimeout(() => table.querySelector('tbody').appendChild(row), 650);
        """);

    Assertions.assertThatThrownBy(() -> model.requiredRow(candidate -> candidate.cell(Header.COMPANY)
            .map(cell -> cell.text().equals("Alfreds")).orElse(false),
        "late row", Duration.ofMillis(450)))
        .isInstanceOf(dev.quokkify.elements.table.model.TableRowNotFoundException.class)
        .hasMessageContaining("late row")
        .hasMessageContaining("PT0.45S");
  }

  @Test(description = "Public custom adapter isolates nested grids and preserves logical cells")
  public void supportsCustomDivAdapter() {
    openFixture();
    TableDomAdapter adapter = customGridAdapter();
    TableModel<Header> model = SelenideDomTableModel.of(
        Selenide.$("#custom-grid"), adapter,
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));
    TableModel<Header> nested = SelenideDomTableModel.of(
        Selenide.$("#nested-custom-grid"), adapter,
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));

    Assertions.assertThat(model.displayedHeaders()).containsExactly("Country", "Company");
    Assertions.assertThat(model.rows()).hasSize(2);
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COMPANY).text()).isEmpty();
    Assertions.assertThat(model.rows().get(1).cell(Header.COMPANY)).isEmpty();
    Assertions.assertThat(nested.rows()).hasSize(1);
    Assertions.assertThat(nested.rows().get(0).requiredCell(Header.COMPANY).text()).isEqualTo("Leak");
  }

  @Test(description = "Custom adapter waits once for a late root and row, then remount-safe handles resolve")
  @SingleThread
  public void customAdapterWaitsAndSurvivesRemount() {
    openFixture();
    TableDomAdapter adapter = customGridAdapter();
    TableModel<Header> model = SelenideDomTableModel.of(
        Selenide.$("#custom-grid"), adapter,
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));
    TableRow<Header> row = model.rows().get(0);
    Selenide.executeJavaScript("window.remountCustomGrid()");
    Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");

    Selenide.executeJavaScript("window.prepareCustomDelayed()");
    TableRow<Header> delayed = model.requiredRow(candidate -> candidate
        .cell(Header.COUNTRY).map(cell -> cell.text().equals("Austria")).orElse(false),
        "custom late row", Duration.ofSeconds(2));
    Assertions.assertThat(delayed.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test(description = "Classic adapter preserves tables whose header row is inside tbody")
  public void supportsBodyOnlyClassicTable() {
    openFixture();
    TableModel<Header> model = SelenideDomTableModel.of(
        Selenide.$("#body-only-classic"), TableDomAdapters.classic(),
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));

    Assertions.assertThat(model.displayedHeaders()).containsExactly("Country", "Company");
    Assertions.assertThat(model.rows()).hasSize(1);
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COMPANY).text())
        .isEqualTo("Alfreds");
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COUNTRY).text())
        .isEqualTo("Austria");
  }

  @Test(description = "Classic adapter excludes rows belonging to a nested table")
  public void excludesNestedTableRows() {
    openFixture();
    TableModel<Header> model = SelenideDomTableModel.of(
        Selenide.$("#nested-classic"), TableDomAdapters.classic(),
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));

    Assertions.assertThat(model.rows()).hasSize(1);
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COMPANY).text())
        .contains("Outer");
  }

  @Test(description = "Classic adapter can select a nested table as its own model root")
  public void supportsNestedClassicRoot() {
    openFixture();
    TableModel<Header> model = SelenideDomTableModel.of(
        Selenide.$("#nested-classic table"), TableDomAdapters.classic(),
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));

    Assertions.assertThat(model.rows()).hasSize(1);
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COUNTRY).text())
        .isEqualTo("Nested");
    Assertions.assertThat(model.rows().get(0).requiredCell(Header.COMPANY).text())
        .isEqualTo("Leak");
  }

  @Test(description = "Generic ARIA adapter addresses role-based grids and survives root remount")
  public void supportsAriaGridAndRemount() {
    openFixture();
    TableModel<Header> model = SelenideDomTableModel.of(
        Selenide.$("#aria-grid"), TableDomAdapters.ariaGrid(),
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));
    TableRow<Header> row = model.rows().get(0);

    Assertions.assertThat(model.displayedHeaders()).containsExactly("Country", "Company");
    Assertions.assertThat(row.requiredCell(Header.COMPANY).text()).isEqualTo("Alfreds");
    Selenide.executeJavaScript("window.remountAriaGrid()");
    Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test(description = "Headerless and repeated headers preserve typed lookup failures")
  public void handlesHeaderlessAndRepeatedHeaders() {
    openFixture();
    TableDomAdapter headerlessAdapter = TableDomAdapters.of(
        By.cssSelector(":scope > .data-row"), By.cssSelector(":scope > .cell"),
        NoTableHeaders.instance());
    TableModel<Header> headerless = SelenideDomTableModel.of(
        Selenide.$("#headerless-grid"), headerlessAdapter,
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));
    TableModel<Header> repeated = SelenideDomTableModel.of(
        Selenide.$("#custom-repeated-grid"), customGridAdapter(),
        DisplayedHeaderResolver.requiringNonNull(header -> header.displayed));

    Assertions.assertThat(headerless.displayedHeaders()).isEmpty();
    Assertions.assertThatThrownBy(() -> headerless.rows().get(0).cell(Header.COUNTRY))
        .isInstanceOf(TableColumnNotFoundException.class);
    Assertions.assertThatThrownBy(() -> repeated.rows().get(0).cell(Header.COMPANY))
        .isInstanceOf(TableColumnAmbiguousException.class);
    Assertions.assertThatThrownBy(() -> SelenideTableQuery.<Header>of(
            Selenide.$("#custom-repeated-grid"), customGridAdapter(), header -> header.displayed)
        .uniqueRow(candidate -> true))
        .isInstanceOf(dev.quokkify.elements.table.model.TableRowAmbiguousException.class)
        .hasMessageContaining("found 2");
  }

  private static TableDomAdapter customGridAdapter() {
    return TableDomAdapters.of(
        By.cssSelector(":scope > .data-row"),
        By.cssSelector(":scope > .cell:not([hidden])"),
        new TableHeaderRowLocator(
            By.cssSelector(":scope > .header-row"),
            By.cssSelector(":scope > .cell:not([hidden])")));
  }

  private static void openFixture() {
    String baseUrl = System.getenv().getOrDefault("NGINX_BASE_URL", "http://localhost");
    Selenide.open(baseUrl + "/table-model-contract/");
  }

  private static final class FixturePage {
    @FindBy(how = How.ID, using = "classic")
    private Table<Header> classic;
    @FindBy(how = How.ID, using = "dynamic")
    private DynamicTable<DynamicHeader> dynamic;
    @FindBy(how = How.ID, using = "flex")
    private FlexTable<Header> flex;
    @FindBy(how = How.ID, using = "horizontal")
    private HorizontalTable<HorizontalHeader> horizontal;
    @FindBy(how = How.ID, using = "dynamic-horizontal")
    private DynamicHorizontalTable<DynamicHorizontalHeader> dynamicHorizontal;
  }

  private enum DynamicHeader implements ConstantFormat {
    COMPANY, COUNTRY;

    @Override
    public String formatValue() {
      return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
  }

  private enum HorizontalHeader implements ConstantFormat {
    COUNTRY, COMPANY;

    @Override
    public String formatValue() {
      return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
  }

  private enum DynamicHorizontalHeader implements ConstantFormat {
    COMPANY, COUNTRY;

    @Override
    public String formatValue() {
      return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
  }

  private static TableModel<Header> model(List<String> headers) {
    return new TableModel<>() {
      @Override
      public List<String> displayedHeaders() {
        return headers;
      }

      @Override
      public List<? extends TableRow<Header>> rows() {
        return List.of();
      }
    };
  }

  private record BackendRow(Map<Header, String> values) implements TableRow<Header> {
    @Override
    public Optional<BackendCell> cell(Header column) {
      return Optional.ofNullable(values.get(column)).map(value -> new BackendCell(column, value));
    }
  }

  private record BackendCell(Header column, String text) implements TableCell<Header> {
  }
}
