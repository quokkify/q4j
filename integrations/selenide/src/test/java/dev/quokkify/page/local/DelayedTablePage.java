package dev.quokkify.page.local;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import dev.quokkify.elements.table.classic.DynamicTable;
import dev.quokkify.elements.table.classic.FlexTable;
import dev.quokkify.elements.table.classic.Row;
import dev.quokkify.elements.table.classic.Table;
import dev.quokkify.elements.table.horizontal.DynamicHorizontalTable;
import dev.quokkify.elements.table.horizontal.HorizontalRow;
import dev.quokkify.elements.table.horizontal.HorizontalTable;
import dev.quokkify.impl.Page;
import dev.quokkify.model.ConstantFormat;

import com.codeborne.selenide.Selenide;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class DelayedTablePage implements Page {

  @FindBy(how = How.ID, using = "customers")
  private Table<Header> table;

  @FindBy(how = How.ID, using = "customers")
  private DynamicTable<DynamicHeader> dynamicTable;

  @FindBy(how = How.ID, using = "flex-customers")
  private FlexTable<Header> flexTable;

  @FindBy(how = How.ID, using = "horizontal-customers")
  private HorizontalTable<HorizontalHeader> horizontalTable;

  @FindBy(how = How.ID, using = "horizontal-customers")
  private DynamicHorizontalTable<HorizontalHeader> dynamicHorizontalTable;

  public Row<Header> getTableRow(Header header, String cellValue, Duration timeout) {
    return table.getRow(header, cellValue, timeout);
  }

  public Row<Header> getTableRow(Header header, String cellValue) {
    return table.getRow(header, cellValue);
  }

  public Row<Header> getTableRow(Map<Header, String> expectedRowValues, Duration timeout) {
    return table.getRow(expectedRowValues, timeout);
  }

  public Row<Header> getTableRow(Map<Header, String> expectedRowValues) {
    return table.getRow(expectedRowValues);
  }

  public Row<Header> getTableRowByPattern(Header header, String pattern, Duration timeout) {
    return table.getRowByPattern(header, pattern, timeout);
  }

  public Row<Header> getTableRowByPattern(Header header, String pattern) {
    return table.getRowByPattern(header, pattern);
  }

  public boolean isTableRowExist(Header header, String cellValue) {
    return table.isRowExist(header, cellValue);
  }

  public Row<DynamicHeader> getDynamicTableRow(DynamicHeader header, String cellValue, Duration timeout) {
    return dynamicTable.getRow(header, cellValue, timeout);
  }

  public Row<Header> getFlexTableRow(Header header, String cellValue, Duration timeout) {
    return flexTable.getRow(header, cellValue, timeout);
  }

  public List<String> getTableColumnValues(Header header) {
    return table.getAllColumnValuesByXpath(header);
  }

  public List<String> getFlexTableColumnValues(Header header) {
    return flexTable.getAllColumnValuesByXpath(header);
  }

  public HorizontalRow<HorizontalHeader> getHorizontalTableRow(HorizontalHeader header, Duration timeout) {
    return horizontalTable.getRow(header, timeout);
  }

  public HorizontalRow<HorizontalHeader> getHorizontalTableRow(HorizontalHeader header) {
    return horizontalTable.getRow(header);
  }

  public HorizontalRow<HorizontalHeader> getDynamicHorizontalTableRow(HorizontalHeader header, Duration timeout) {
    return dynamicHorizontalTable.getRow(header, timeout);
  }

  public boolean isHorizontalTableRowExist(HorizontalHeader header) {
    return horizontalTable.isRowExist(header);
  }

  public boolean isHorizontalTableRowExist(String header) {
    return horizontalTable.isRowExist(header);
  }

  public Map<String, String> getHorizontalTableValues() {
    return horizontalTable.columnsAndValuesAsMap();
  }

  public void reloadClassicTable() {
    Selenide.executeJavaScript("window.reloadClassicTable()");
  }

  public void reloadFlexTable() {
    Selenide.executeJavaScript("window.reloadFlexTable()");
  }

  public void reloadHorizontalTable() {
    Selenide.executeJavaScript("window.reloadHorizontalTable()");
  }

  public enum Header {
    COMPANY, CONTACT, COUNTRY
  }

  public enum DynamicHeader implements ConstantFormat {
    COUNTRY, COMPANY, CONTACT;

    @Override
    public String formatValue() {
      return name();
    }
  }

  public enum HorizontalHeader implements ConstantFormat {
    NAME, TELEPHONE_1, TELEPHONE_2;

    @Override
    public String formatValue() {
      return name();
    }
  }
}
