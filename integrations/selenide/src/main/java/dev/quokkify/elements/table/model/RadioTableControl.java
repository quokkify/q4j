package dev.quokkify.elements.table.model;

/** Explicit capability for a radio control, which can only be selected. */
public interface RadioTableControl extends TableControl {

  RadioTableControl select();

  boolean isSelected();

  /** Radio buttons cannot be deselected independently. */
  default RadioTableControl setSelected(boolean selected) {
    if (!selected) {
      throw new UnsupportedTableEditException("Radio controls cannot be unselected");
    }
    return select();
  }
}
