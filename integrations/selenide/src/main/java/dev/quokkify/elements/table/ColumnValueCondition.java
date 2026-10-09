package dev.quokkify.elements.table;

import java.util.List;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.WebElementCondition;
import org.openqa.selenium.WebElement;

final class ColumnValueCondition extends WebElementCondition {

  private final Table table;
  private final String column;
  private final String value;

  ColumnValueCondition(Table table, String column, String value) {
    super(column + " = \"" + value + "\"");
    this.table = table;
    this.column = column;
    this.value = value;
  }

  @Override
  public CheckResult check(Driver driver, WebElement row) {
    List<String> headers = table.headers().texts();
    if (headers.isEmpty()) {
      return CheckResult.rejected("no headers displayed yet", headers);
    }
    int index = ColumnResolver.indexOf(headers, column, table.root().toString());
    List<WebElement> cells = row.findElements(table.layout().cells());
    if (index >= cells.size()) {
      return CheckResult.rejected("row has " + cells.size() + " cells", cells.size());
    }
    String actual = cells.get(index).getText().trim();
    return new CheckResult(actual.equals(value), actual);
  }
}
