package dev.quokkify.elements.table.model;

import java.util.Objects;

import org.openqa.selenium.By;

/** Locates a displayed header cell within each data row, as in a horizontal table. */
public record RowHeaderCellLocator(By headerCellLocator) implements TableHeaderLocator {

  public RowHeaderCellLocator {
    Objects.requireNonNull(headerCellLocator, "headerCellLocator");
  }
}
