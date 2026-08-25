package dev.quokkify.elements.table.model;

import java.util.Objects;
import java.util.function.Supplier;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.SetValueOptions;
import com.codeborne.selenide.WebElementCondition;
import org.openqa.selenium.By;

/** Internal lazy Selenide implementation shared by indexed and typed cell references. */
class ElementTableCellRef<C> implements TableCellRef<C> {

  private static final String EDITABLE_INPUT_SELECTOR =
      "input:not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit])"
          + ":not([type=reset]):not([type=image]):not([type=file]):not([type=hidden]), textarea";

  private final int rowIndex;
  private final int columnIndex;
  private final Supplier<SelenideElement> element;
  private final String description;

  ElementTableCellRef(int rowIndex, int columnIndex, Supplier<SelenideElement> element,
                      String description) {
    this.rowIndex = rowIndex;
    this.columnIndex = columnIndex;
    this.element = Objects.requireNonNull(element, "element");
    this.description = Objects.requireNonNull(description, "description");
  }

  @Override
  public int rowIndex() {
    return rowIndex;
  }

  @Override
  public int columnIndex() {
    return columnIndex;
  }

  @Override
  public String text() {
    return element.get().text();
  }

  @Override
  public TableCellRef<C> shouldHave(WebElementCondition... conditions) {
    element.get().shouldHave(conditions);
    return this;
  }

  @Override
  public TableCellRef<C> shouldBe(WebElementCondition... conditions) {
    element.get().shouldBe(conditions);
    return this;
  }

  @Override
  public TableCellRef<C> click() {
    element.get().click();
    return this;
  }

  @Override
  public EditableTableControl input() {
    return new EditableControl(() -> element.get().find(By.cssSelector(EDITABLE_INPUT_SELECTOR)),
        description + " input");
  }

  @Override
  public SelectTableControl select() {
    return new SelectControl(() -> element.get().find(By.tagName("select")), description + " select");
  }

  @Override
  public CheckableTableControl checkbox() {
    return new CheckableControl(() -> element.get().find(By.cssSelector("input[type=checkbox]")),
        description + " checkbox");
  }

  @Override
  public RadioTableControl radio() {
    return new RadioControl(() -> element.get().find(By.cssSelector("input[type=radio]")),
        description + " radio");
  }

  @Override
  public TableControl button() {
    return new ElementControl(() -> element.get().find(By.cssSelector("button, input[type=button], input[type=submit]")),
        description + " button");
  }

  @Override
  public TableControl link() {
    return new ElementControl(() -> element.get().find(By.tagName("a")), description + " link");
  }

  @Override
  public EditableTableControl editable() {
    SelenideElement cell = element.get();
    if (Boolean.parseBoolean(cell.getDomProperty("isContentEditable"))) {
      return new ContentEditableControl(element, description + " contenteditable cell");
    }
    SelenideElement control = cell.find(By.cssSelector(EDITABLE_INPUT_SELECTOR));
    if (control.exists()) {
      return new EditableControl(() -> element.get().find(By.cssSelector(EDITABLE_INPUT_SELECTOR)),
          description + " editable control");
    }
    throw new UnsupportedTableEditException(description);
  }

  private static class ElementControl implements TableControl {
    protected final Supplier<SelenideElement> element;
    private final String description;

    ElementControl(Supplier<SelenideElement> element, String description) {
      this.element = Objects.requireNonNull(element, "element");
      this.description = Objects.requireNonNull(description, "description");
    }

    @Override
    public TableControl shouldHave(WebElementCondition... conditions) {
      element.get().shouldHave(conditions);
      return this;
    }

    @Override
    public TableControl shouldBe(WebElementCondition... conditions) {
      element.get().shouldBe(conditions);
      return this;
    }

    @Override
    public TableControl click() {
      element.get().click();
      return this;
    }

    @Override
    public String text() {
      return element.get().text();
    }

    @Override
    public String toString() {
      return description;
    }
  }

  private static final class EditableControl extends ElementControl implements EditableTableControl {
    private EditableControl(Supplier<SelenideElement> element, String description) {
      super(element, description);
    }

    @Override
    public EditableTableControl setValue(String value) {
      element.get().setValue(value);
      return this;
    }

    @Override
    public String value() {
      return element.get().getValue();
    }
  }

  private static final class CheckableControl extends ElementControl implements CheckableTableControl {
    private CheckableControl(Supplier<SelenideElement> element, String description) {
      super(element, description);
    }

    @Override
    public CheckableTableControl setSelected(boolean selected) {
      element.get().setSelected(selected);
      return this;
    }

    @Override
    public boolean isSelected() {
      return element.get().isSelected();
    }
  }

  private static final class ContentEditableControl extends ElementControl
      implements EditableTableControl {
    private ContentEditableControl(Supplier<SelenideElement> element, String description) {
      super(element, description);
    }

    @Override
    public EditableTableControl setValue(String value) {
      SelenideElement target = element.get();
      target.setValue(SetValueOptions.withText(value));
      return this;
    }

    @Override
    public String value() {
      return element.get().innerText();
    }
  }

  private static final class RadioControl extends ElementControl implements RadioTableControl {
    private RadioControl(Supplier<SelenideElement> element, String description) {
      super(element, description);
    }

    @Override
    public RadioTableControl select() {
      element.get().setSelected(true);
      return this;
    }

    @Override
    public boolean isSelected() {
      return element.get().isSelected();
    }
  }

  private static final class SelectControl extends ElementControl implements SelectTableControl {
    private SelectControl(Supplier<SelenideElement> element, String description) {
      super(element, description);
    }

    @Override
    public SelectTableControl selectOption(String text) {
      element.get().selectOption(text);
      return this;
    }

    @Override
    public String selectedText() {
      return element.get().getSelectedOptionText();
    }
  }
}
