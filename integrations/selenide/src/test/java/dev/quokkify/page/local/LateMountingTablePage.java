package dev.quokkify.page.local;

import java.time.Duration;

import dev.quokkify.elements.table.classic.Row;
import dev.quokkify.elements.table.classic.Table;
import dev.quokkify.impl.Page;

import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class LateMountingTablePage implements Page {

  @FindBy(how = How.ID, using = "empty-customers")
  private Table<Header> emptyTable;

  @FindBy(how = How.ID, using = "late-customers")
  private Table<Header> lateTable;

  public Row<Header> getEmptyTableRow(Header header, String cellValue, Duration timeout) {
    return emptyTable.getRow(header, cellValue, timeout);
  }

  public Row<Header> getLateTableRow(Header header, String cellValue, Duration timeout) {
    return lateTable.getRow(header, cellValue, timeout);
  }

  public boolean isLateTableRowExist(Header header, String cellValue) {
    return lateTable.isRowExist(header, cellValue);
  }

  public enum Header {
    COMPANY, CONTACT, COUNTRY
  }
}
