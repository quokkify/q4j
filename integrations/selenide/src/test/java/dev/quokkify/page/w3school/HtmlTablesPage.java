package dev.quokkify.page.w3school;

import java.util.Map;

import dev.quokkify.annotation.PageUrl;
import dev.quokkify.elements.table.classic.Row;
import dev.quokkify.elements.table.classic.Table;

import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

@PageUrl("/table")
public class HtmlTablesPage extends BaseTablePage {

  @FindBy(how = How.ID, using = "customers")
  private Table<Header> table;

  public Row<Header> getTableRowWithValueInColumn(String company) {
    return table.getRow(Header.COMPANY, company);
  }

  public Row<Header> getTableRowWithValuesInColumns(Map<Header, String> expectedRowValues) {
    return table.getRow(expectedRowValues);
  }

  public enum Header {
    COMPANY, CONTACT, COUNTRY
  }
}
