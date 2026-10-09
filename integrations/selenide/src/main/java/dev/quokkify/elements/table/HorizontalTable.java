package dev.quokkify.elements.table;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import org.openqa.selenium.By;

public final class HorizontalTable {

  private static final String OWN_ROWS = "./tr | ./tbody/tr | ./thead/tr | ./tfoot/tr";

  private final SelenideElement root;

  private HorizontalTable(SelenideElement root) {
    this.root = root;
  }

  public static HorizontalTable of(SelenideElement root) {
    return new HorizontalTable(root);
  }

  public ElementsCollection headers() {
    return root.$$x("(" + OWN_ROWS + ")/th");
  }

  public SelenideElement value(String header) {
    WebElementCondition hasHeader = Condition.match("header = \"" + header + "\"",
        tr -> tr.findElements(By.xpath("./th")).stream().anyMatch(th -> th.getText().trim().equals(header)));
    return root.$$x(OWN_ROWS).findBy(hasHeader).$x("./td");
  }
}
