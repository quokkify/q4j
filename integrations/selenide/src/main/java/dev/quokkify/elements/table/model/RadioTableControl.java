package dev.quokkify.elements.table.model;

/** Explicit capability for a radio control, which can only be selected. */
public interface RadioTableControl extends TableControl {

  /** Selects this radio; independent deselection is intentionally not part of this contract. */
  RadioTableControl select();

  boolean isSelected();
}
