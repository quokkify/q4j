package dev.quokkify.elements.table;

import java.util.List;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ColumnResolverTest {

  @Test
  public void resolvesTrimmedExactHeader() {
    assertThat(ColumnResolver.indexOf(List.of("Country", " Company \n"), "Company", "#t")).isEqualTo(1);
  }

  @Test
  public void normalizesWhitespaceRuns() {
    assertThat(ColumnResolver.indexOf(List.of("Country", "Company  Name"), "Company Name", "#t")).isEqualTo(1);
    assertThat(ColumnResolver.indexOf(List.of("Country", "Company\u00A0\n Name"), " Company Name", "#t"))
        .isEqualTo(1);
  }

  @Test
  public void isCaseSensitive() {
    assertThatThrownBy(() -> ColumnResolver.indexOf(List.of("company"), "Company", "#t"))
        .isInstanceOf(TableColumnException.class);
  }

  @Test
  public void reportsMissingHeaderWithDisplayedHeaders() {
    assertThatThrownBy(() -> ColumnResolver.indexOf(List.of("Country", "Company"), "Region", "#t"))
        .isInstanceOf(TableColumnException.class)
        .hasMessage("Column \"Region\" not found in #t; displayed headers: [Country, Company]");
  }

  @Test
  public void reportsAmbiguousHeader() {
    assertThatThrownBy(() -> ColumnResolver.indexOf(List.of("Country", "Company", "Company"), "Company", "#t"))
        .isInstanceOf(TableColumnException.class)
        .hasMessage("Column \"Company\" ambiguous in #t; displayed headers: [Country, Company, Company]");
  }
}
