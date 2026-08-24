package dev.quokkify.test;

import java.util.List;
import java.util.Optional;

import dev.quokkify.elements.table.model.DisplayedHeaderResolver;
import dev.quokkify.elements.table.model.TableCell;
import dev.quokkify.elements.table.model.TableColumnNotFoundException;
import dev.quokkify.elements.table.model.TableModel;
import dev.quokkify.elements.table.model.TableRow;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class TableModelContractTest {

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
