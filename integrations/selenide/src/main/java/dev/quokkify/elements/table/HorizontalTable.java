package dev.quokkify.elements.table;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import org.openqa.selenium.By;

public final class HorizontalTable {

  private final SelenideElement root;

  private HorizontalTable(SelenideElement root) {
    this.root = root;
  }

  public static HorizontalTable of(SelenideElement root) {
    return new HorizontalTable(root);
  }

  public ElementsCollection headers() {
    return root.$$x(".//tr/th");
  }

  public SelenideElement value(String header) {
    WebElementCondition hasHeader = Condition.match("header = \"" + header + "\"",
        tr -> tr.findElements(By.xpath("./th")).stream().anyMatch(th -> th.getText().trim().equals(header)));
    return root.$$x(".//tr").findBy(hasHeader).$x("./td");
  }
}
