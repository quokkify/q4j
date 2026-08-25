package dev.quokkify.elements.table.model;

/** Explicit capability for an HTML select embedded in a table cell. */
public interface SelectTableControl extends TableControl {

  SelectTableControl selectOption(String text);

  String selectedText();
}
