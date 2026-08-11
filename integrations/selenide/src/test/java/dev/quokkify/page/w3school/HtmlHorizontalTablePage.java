package dev.quokkify.page.w3school;

import dev.quokkify.annotation.PageUrl;
import dev.quokkify.elements.table.horizontal.HorizontalRow;
import dev.quokkify.elements.table.horizontal.HorizontalTable;
import dev.quokkify.model.ConstantFormat;

import com.codeborne.selenide.Selenide;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

@PageUrl("/horizontal_table")
public class HtmlHorizontalTablePage extends BaseTablePage {

  @FindBy(how = How.XPATH, using = "//table[2]")
  private HorizontalTable<Header> table;

  public HtmlHorizontalTablePage() {
    Selenide.switchTo().frame("iframeResult");
  }

  public HorizontalRow<Header> getTableRowByColumn(Header header) {
    return table.getRow(header);
  }

  public enum Header implements ConstantFormat {
    NAME, TELEPHONE_1, TELEPHONE_2;

    @Override
    public String formatValue() {
      return name();
    }
  }
}
