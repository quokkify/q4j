package dev.quokkify.elements.table.classic;

import java.util.function.Function;

import dev.quokkify.elements.table.classic.base.BaseClassicTable;
import dev.quokkify.html.model.HtmlTag;
import dev.quokkify.model.ConstantFormat;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Dynamic Table UI element and methods of working with it.
 * Table has a variable number of columns.
 *
 * @param <T> enum with columns enumerations
 */
public class DynamicTable<T extends Enum<T> & ConstantFormat> extends BaseClassicTable<T> {

  /**
   * Fetch column index by column title text in the UI table.
   */
  @Override
  protected Function<T, Integer> fetchColumnIndex() {
    return column -> getSelf().shouldBe(Condition.visible)
        .findAll(By.xpath(".//%s//%s".formatted(HtmlTag.THEAD, HtmlTag.TH)))
        .findBy(Condition.text(column.capitalize()))
        .findAll(By.xpath("./preceding-sibling::%s".formatted(HtmlTag.TH)))
        .size();
  }

  @Override
  protected int fetchColumnIndex(Driver driver, WebElement table, T columnHeader) {
    for (WebElement header : table.findElements(By.tagName(HtmlTag.TH))) {
      CheckResult result = Condition.text(columnHeader.capitalize()).check(driver, header);
      if (result.verdict() == CheckResult.Verdict.ACCEPT) {
        return header.findElements(By.xpath("./preceding-sibling::%s".formatted(HtmlTag.TH))).size();
      }
    }
    return -1;
  }
}
