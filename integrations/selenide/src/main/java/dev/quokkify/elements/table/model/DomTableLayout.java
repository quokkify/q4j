package dev.quokkify.elements.table.model;

/**
 * DOM layout of a table. Layout describes markup only; it does not describe table capabilities.
 */
public enum DomTableLayout {
  /** Header cells are in a header row and data cells are in subsequent rows. */
  CLASSIC,
  /** Header and data rows are flex containers with div cells. */
  FLEX,
  /** Each row starts with a header cell followed by its value cell. */
  HORIZONTAL
}
