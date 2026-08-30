package dev.quokkify.test;

import java.util.Optional;

import dev.quokkify.elements.table.model.RowConditions;
import dev.quokkify.elements.table.model.TableQueryRow;
import dev.quokkify.elements.table.model.TypedTableCellRef;

import org.assertj.core.api.Assertions;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class RowConditionsContractTest {

  private enum Header {
    VALUE
  }

  @Test(description = "greaterThan accepts canonical BigDecimal text and rejects decorated or malformed text")
  public void greaterThanUsesStrictNumericContract() {
    String[] accepted = {"11", "10.01", "+11", "1e2"};
    for (String value : accepted) {
      Assertions.assertThat(RowConditions.greaterThan(Header.VALUE, 10)
          .test(numericRow(value))).as(value).isTrue();
    }
    String[] rejected = {"$100", "10%", "1,000", "", "ten"};
    for (String value : rejected) {
      Assertions.assertThat(RowConditions.greaterThan(Header.VALUE, 10)
          .test(numericRow(value))).as(value).isFalse();
    }
  }

  @SuppressWarnings("unchecked")
  private static TableQueryRow<Header> numericRow(String value) {
    TableQueryRow<Header> row = Mockito.mock(TableQueryRow.class);
    TypedTableCellRef<Header> cell = Mockito.mock(TypedTableCellRef.class);
    Mockito.when(cell.text()).thenReturn(value);
    Mockito.doReturn(Optional.of(cell)).when(row).cell(Header.VALUE);
    return row;
  }
}
