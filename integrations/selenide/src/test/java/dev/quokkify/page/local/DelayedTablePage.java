package dev.quokkify.page.local;

import java.time.Duration;
import java.util.Map;

import dev.quokkify.elements.table.classic.Row;
import dev.quokkify.elements.table.classic.Table;
import dev.quokkify.elements.table.horizontal.DynamicHorizontalTable;
import dev.quokkify.elements.table.horizontal.HorizontalRow;
import dev.quokkify.elements.table.horizontal.HorizontalTable;
import dev.quokkify.impl.Page;
import dev.quokkify.model.ConstantFormat;

import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class DelayedTablePage implements Page {

  @FindBy(how = How.ID, using = "customers")
  private Table<Header> table;

  @FindBy(how = How.ID, using = "horizontal-customers")
  private HorizontalTable<HorizontalHeader> horizontalTable;

  @FindBy(how = How.ID, using = "horizontal-customers")
  private DynamicHorizontalTable<HorizontalHeader> dynamicHorizontalTable;

  public Row<Header> getTableRow(Header header, String cellValue, Duration timeout, Duration pollingInterval) {
    return table.getRow(header, cellValue, timeout, pollingInterval);
  }

  public Row<Header> getTableRow(Header header, String cellValue) {
    return table.getRow(header, cellValue);
  }

  public Row<Header> getTableRow(Map<Header, String> expectedRowValues, Duration timeout, Duration pollingInterval) {
    return table.getRow(expectedRowValues, timeout, pollingInterval);
  }

  public Row<Header> getTableRow(Map<Header, String> expectedRowValues) {
    return table.getRow(expectedRowValues);
  }

  public Row<Header> getTableRowByPattern(Header header, String pattern, Duration timeout, Duration pollingInterval) {
    return table.getRowByPattern(header, pattern, timeout, pollingInterval);
  }

  public Row<Header> getTableRowByPattern(Header header, String pattern) {
    return table.getRowByPattern(header, pattern);
  }

  public boolean isTableRowExist(Header header, String cellValue) {
    return table.isRowExist(header, cellValue);
  }

  public HorizontalRow<HorizontalHeader> getHorizontalTableRow(HorizontalHeader header, Duration timeout,
                                                               Duration pollingInterval) {
    return horizontalTable.getRow(header, timeout, pollingInterval);
  }

  public HorizontalRow<HorizontalHeader> getHorizontalTableRow(HorizontalHeader header) {
    return horizontalTable.getRow(header);
  }

  public HorizontalRow<HorizontalHeader> getDynamicHorizontalTableRow(HorizontalHeader header, Duration timeout,
                                                                      Duration pollingInterval) {
    return dynamicHorizontalTable.getRow(header, timeout, pollingInterval);
  }

  public boolean isHorizontalTableRowExist(HorizontalHeader header) {
    return horizontalTable.isRowExist(header);
  }

  public enum Header {
    COMPANY, CONTACT, COUNTRY
  }

  public enum HorizontalHeader implements ConstantFormat {
    NAME, TELEPHONE_1, TELEPHONE_2;

    @Override
    public String formatValue() {
      return name();
    }
  }
}
