package dev.quokkify.elements.table.model;

import com.codeborne.selenide.WebElementCondition;

/** Lazy element-backed control embedded in a table cell. */
public interface TableControl {

  TableControl shouldHave(WebElementCondition... conditions);

  TableControl shouldBe(WebElementCondition... conditions);

  TableControl click();

  String text();
}
