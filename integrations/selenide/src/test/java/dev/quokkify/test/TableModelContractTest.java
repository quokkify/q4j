package dev.quokkify.test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import dev.quokkify.elements.table.classic.DynamicTable;
import dev.quokkify.elements.table.classic.FlexTable;
import dev.quokkify.elements.table.classic.Table;
import dev.quokkify.elements.table.horizontal.DynamicHorizontalTable;
import dev.quokkify.elements.table.horizontal.HorizontalTable;
import dev.quokkify.elements.table.model.DisplayedHeaderResolver;
import dev.quokkify.elements.table.model.TableCell;
import dev.quokkify.elements.table.model.TableCellNotFoundException;
import dev.quokkify.elements.table.model.TableColumnNotFoundException;
import dev.quokkify.elements.table.model.TableModel;
import dev.quokkify.elements.table.model.TableRow;
import dev.quokkify.model.ConstantFormat;

import com.codeborne.selenide.Selenide;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;
import org.testng.annotations.Test;

public class TableModelContractTest extends BaseTest {

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

  @Test(description = "Every legacy table variant exposes the neutral model and typed cells")
  public void bridgesAllLegacyVariants() {
    openFixture();
    FixturePage page = Selenide.page(FixturePage.class);

    Assertions.assertThat(page.classic.asDomModel(h -> h.displayed).rows().get(0)
        .requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(page.dynamic.asDomModel(h -> h.formatValue()).displayedHeaders())
        .containsExactly("Company", "Country");
    Assertions.assertThat(page.flex.asDomModel(h -> h.displayed).rows().get(0)
        .requiredCell(Header.COMPANY).text()).isEqualTo("Alfreds");
    Assertions.assertThat(page.horizontal.asDomModel(h -> h.formatValue()).row(
        row -> row.cell(HorizontalHeader.COUNTRY).isPresent()).orElseThrow()
        .requiredCell(HorizontalHeader.COUNTRY).text()).isEqualTo("Austria");
    Assertions.assertThat(page.dynamicHorizontal.asDomModel(h -> h.formatValue()).row(
        row -> row.cell(DynamicHorizontalHeader.COUNTRY).isPresent()).orElseThrow()
        .requiredCell(DynamicHorizontalHeader.COUNTRY).text()).isEqualTo("Austria");
  }

  @Test(description = "Required lookup waits through Selenide and survives a DOM remount")
  public void waitsForAndSurvivesRemount() {
    openFixture();
    FixturePage page = Selenide.page(FixturePage.class);
    TableModel<Header> model = page.classic.asDomModel(h -> h.displayed);
    Selenide.executeJavaScript("window.prepareDelayedRow()");
    Assertions.assertThat(model.row(candidate -> candidate.cell(Header.COMPANY)
        .map(cell -> cell.text().equals("Alfreds")).orElse(false))).isEmpty();
    TableRow<Header> row = model.requiredRow(candidate -> candidate.cell(Header.COMPANY)
        .map(cell -> cell.text().equals("Alfreds")).orElse(false), "company", Duration.ofSeconds(2));

    Selenide.executeJavaScript("window.remount()");
    Assertions.assertThat(row.requiredCell(Header.COUNTRY).text()).isEqualTo("Austria");
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
}
