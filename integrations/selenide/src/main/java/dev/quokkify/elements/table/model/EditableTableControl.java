package dev.quokkify.elements.table.model;

/** Explicit capability for a value-bearing editable table control. */
public interface EditableTableControl extends TableControl {

  EditableTableControl setValue(String value);

  String value();
}
