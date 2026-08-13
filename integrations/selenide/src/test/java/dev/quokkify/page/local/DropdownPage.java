package dev.quokkify.page.local;

import java.time.Duration;
import java.util.List;

import dev.quokkify.elements.single.dropdown.InputDropdown;
import dev.quokkify.elements.single.dropdown.MultiSelectInputDropdown;
import dev.quokkify.impl.Page;

import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class DropdownPage implements Page {

  private static final Duration QUICK_TIMEOUT = Duration.ofMillis(400);
  private static final Duration QUICK_POLLING_INTERVAL = Duration.ofMillis(50);

  @FindBy(how = How.ID, using = "single-open")
  private TestSingleDropdown openSingleDropdown;

  @FindBy(how = How.ID, using = "single-closed")
  private TestSingleDropdown closedSingleDropdown;

  @FindBy(how = How.ID, using = "multi-primary")
  private TestMultiDropdown primaryMultiDropdown;

  @FindBy(how = How.ID, using = "multi-secondary")
  private TestMultiDropdown secondaryMultiDropdown;

  @FindBy(how = How.ID, using = "multi-stuck")
  private StuckMultiDropdown stuckMultiDropdown;

  public TestSingleDropdown openSingleDropdown() {
    return openSingleDropdown;
  }

  public TestSingleDropdown closedSingleDropdown() {
    return closedSingleDropdown;
  }

  public TestMultiDropdown primaryMultiDropdown() {
    return primaryMultiDropdown;
  }

  public TestMultiDropdown secondaryMultiDropdown() {
    return secondaryMultiDropdown;
  }

  public StuckMultiDropdown stuckMultiDropdown() {
    return stuckMultiDropdown;
  }

  public static class TestSingleDropdown extends InputDropdown {

    @Override
    protected By getInputSelector() {
      return By.cssSelector("input[role='combobox']");
    }

    @Override
    protected By getSelectedOptionsSelector() {
      return By.cssSelector(".chip-row .item");
    }

    @Override
    protected By getOptionsContainerSelector() {
      return By.cssSelector("[role='listbox']");
    }

    @Override
    protected By getOptionsSelector() {
      return By.cssSelector("[role='option']");
    }

    public boolean isExpanded() {
      return Boolean.parseBoolean(getInput().getAttribute("aria-expanded"));
    }

    public String selectedLabel() {
      return getSelectedOptionLabelText(getSelf().find(getSelectedOptionsSelector()));
    }
  }

  public static class TestMultiDropdown extends MultiSelectInputDropdown {

    @Override
    protected By getInputSelector() {
      return By.cssSelector("input[role='combobox']");
    }

    @Override
    protected By getSelectedOptionsSelector() {
      return By.cssSelector(".chip-row .item");
    }

    @Override
    protected By getOptionsContainerSelector() {
      return By.cssSelector("[role='listbox']");
    }

    @Override
    protected By getOptionsSelector() {
      return By.cssSelector("[role='option']");
    }

    @Override
    protected By getSelectedOptionLabelSelector() {
      return By.cssSelector("[data-dropdown-label]");
    }

    public String inputValue() {
      return getInput().getValue();
    }

    public void setQuery(String value) {
      type(value);
    }

    public List<String> chipRemoveTexts() {
      return getSelf().findAll(getSelectedOptionsSelector()).stream()
          .map(chip -> chip.find(removeOptionButtonSelector()).getText())
          .toList();
    }

    public boolean isExpanded() {
      return Boolean.parseBoolean(getInput().getAttribute("aria-expanded"));
    }

    public List<String> visibleChipTexts() {
      return getSelf().findAll(getSelectedOptionsSelector()).texts();
    }
  }

  public static class StuckMultiDropdown extends TestMultiDropdown {

    @Override
    protected Duration chipRemovalTimeout() {
      return QUICK_TIMEOUT;
    }

    @Override
    protected Duration chipRemovalPollingInterval() {
      return QUICK_POLLING_INTERVAL;
    }
  }
}
