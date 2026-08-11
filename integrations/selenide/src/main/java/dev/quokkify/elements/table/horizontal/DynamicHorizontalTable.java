package dev.quokkify.elements.table.horizontal;

import java.util.function.Function;

import dev.quokkify.html.model.HtmlTag;
import dev.quokkify.model.ConstantFormat;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Dynamic Horizontal Table UI element and methods of working with it.
 * Table has a variable number of columns.
 *
 * @param <T> enum with columns enumerations
 */
public class DynamicHorizontalTable<T extends Enum<T> & ConstantFormat> extends BaseHorizontalTable<T> {

  /**
   * Fetch column index by column title text in the UI table.
   */
  @Override
  protected Function<T, Integer> fetchColumnIndex() {
    return column -> getSelf().shouldBe(Condition.visible)
        .findAll(By.xpath(".//tr/th"))
        .findBy(Condition.text(column.capitalize()))
        .findAll(By.xpath("./parent::tr/preceding-sibling::tr"))
        .size();
  }

  @Override
  protected int fetchColumnIndex(Driver driver, WebElement table, T columnHeader) {
    var rows = table.findElements(By.tagName(HtmlTag.TR));
    for (int index = 0; index < rows.size(); index++) {
      var headers = rows.get(index).findElements(By.tagName(HtmlTag.TH));
      if (!headers.isEmpty()
          && Condition.text(columnHeader.capitalize()).check(driver, headers.get(0)).verdict()
          == CheckResult.Verdict.ACCEPT) {
        return index;
      }
    }
    return -1;
  }
}
