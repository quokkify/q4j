package dev.quokkify.elements.table;

import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TableLayoutTest {

  @Test
  public void htmlLayoutUsesDirectChildXpath() {
    assertThat(TableLayout.html()).isEqualTo(new TableLayout(
        By.xpath("./tbody/tr[td]"), By.xpath("./*[self::td or self::th]"),
        By.xpath("./thead/tr[th][last()]/*[self::th or self::td]"
            + " | ./thead[not(tr/th)]/tr[last()]/*[self::th or self::td]")));
  }

  @Test
  public void extractsXpathExpression() {
    assertThat(TableLayout.xpath(By.xpath("./tbody/tr"))).contains("./tbody/tr");
    assertThat(TableLayout.xpath(By.cssSelector("tr"))).isEmpty();
  }

  @Test
  public void ariaLayoutUsesExplicitRoles() {
    assertThat(TableLayout.aria()).isEqualTo(new TableLayout(
        By.xpath(".//*[@role='row'][*[@role='cell' or @role='gridcell']]"),
        By.xpath("./*[@role='cell' or @role='gridcell' or @role='rowheader']"),
        By.xpath(".//*[@role='columnheader']")));
  }

  @Test
  public void rejectsNullLocators() {
    assertThatThrownBy(() -> TableLayout.of(null, By.id("c"), By.id("h")))
        .isInstanceOf(NullPointerException.class).hasMessage("rows");
  }
}
