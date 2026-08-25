package dev.quokkify.elements.table.model;

import java.util.Objects;

import org.openqa.selenium.By;

/** Locates one header row and the displayed header cells within that row. */
public record TableHeaderRowLocator(By headerRowLocator, By headerCellLocator)
    implements TableHeaderLocator {

  public TableHeaderRowLocator {
    Objects.requireNonNull(headerRowLocator, "headerRowLocator");
    Objects.requireNonNull(headerCellLocator, "headerCellLocator");
  }
}
