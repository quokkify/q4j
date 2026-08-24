package dev.quokkify.elements.table.model;

import com.codeborne.selenide.WebElementCondition;

/** Lazy reference to a cell addressed by zero-based row and column indexes. */
public interface TableCellRef<C> {

  int rowIndex();

  int columnIndex();

  String text();

  /** Waits until the current cell satisfies Selenide element conditions. */
  default TableCellRef<C> shouldHave(WebElementCondition... conditions) {
    throw elementBackedOperation("shouldHave");
  }

  /** Waits until the current cell satisfies Selenide element state conditions. */
  default TableCellRef<C> shouldBe(WebElementCondition... conditions) {
    throw elementBackedOperation("shouldBe");
  }

  /** Clicks the current cell after resolving it from the table root. */
  default TableCellRef<C> click() {
    throw elementBackedOperation("click");
  }

  /** Resolves an input or textarea embedded in this cell. */
  default EditableTableControl input() {
    throw elementBackedOperation("input");
  }

  /** Resolves a select embedded in this cell. */
  default SelectTableControl select() {
    throw elementBackedOperation("select");
  }

  /** Resolves a checkbox embedded in this cell. */
  default CheckableTableControl checkbox() {
    throw elementBackedOperation("checkbox");
  }

  /** Resolves a radio button embedded in this cell. */
  default CheckableTableControl radio() {
    throw elementBackedOperation("radio");
  }

  /** Resolves a button embedded in this cell. */
  default TableControl button() {
    throw elementBackedOperation("button");
  }

  /** Resolves a link embedded in this cell. */
  default TableControl link() {
    throw elementBackedOperation("link");
  }

  /** Resolves an explicitly editable cell or rejects a read-only cell. */
  default EditableTableControl editable() {
    throw elementBackedOperation("editable");
  }

  private UnsupportedOperationException elementBackedOperation(String operation) {
    return new UnsupportedOperationException(
        operation + " requires a Selenide element-backed table cell reference");
  }
}
