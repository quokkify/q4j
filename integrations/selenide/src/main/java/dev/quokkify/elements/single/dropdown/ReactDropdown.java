package dev.quokkify.elements.single.dropdown;

import dev.quokkify.html.model.HtmlTag;

import org.openqa.selenium.By;

/**
 * React Dropdown menu looking like a button.
 */
public class ReactDropdown extends AbstractDropdown {

  @Override
  protected By getOptionsContainerSelector() {
    return By.xpath("./ancestor::%s".formatted(HtmlTag.BODY));
  }
}
