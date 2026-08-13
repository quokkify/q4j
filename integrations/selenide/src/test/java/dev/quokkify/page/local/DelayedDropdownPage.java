package dev.quokkify.page.local;

import dev.quokkify.elements.single.dropdown.InputDropdown;
import dev.quokkify.impl.Page;

import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class DelayedDropdownPage implements Page {

  @FindBy(how = How.ID, using = "custom-dropdown")
  private DelayedFruitDropdown dropdown;

  public void selectDelayedFruit(String fruit) {
    dropdown.selectOptionByExactText(fruit);
  }

  public String getSelectedFruit() {
    return dropdown.getSelectedOptionText();
  }

  public boolean isDropdownClosed() {
    return dropdown.isClosed();
  }

  public static final class DelayedFruitDropdown extends InputDropdown {
    @Override
    protected By getInputSelector() {
      return By.id("custom-dropdown-input");
    }

    @Override
    protected By getOptionsContainerSelector() {
      return By.id("custom-dropdown-listbox");
    }

    @Override
    public void toggle() {
      getSelf().find(By.id("custom-dropdown-toggle")).click();
    }
  }
}
