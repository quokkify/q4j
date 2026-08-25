package dev.quokkify.elements.table.model;

import java.util.Objects;

import org.openqa.selenium.By;

/**
 * Immutable DOM adapter for one table shape.
 *
 * <p>The locators are evaluated relative to the current table or row on every operation. They
 * therefore define both the addressable data and the hidden-column policy of the model.</p>
 */
public record TableDomAdapter(By mountedDataRowLocator, By dataCellLocator,
                              TableHeaderLocator headerLocator) {

  public TableDomAdapter {
    Objects.requireNonNull(mountedDataRowLocator, "mountedDataRowLocator");
    Objects.requireNonNull(dataCellLocator, "dataCellLocator");
    Objects.requireNonNull(headerLocator, "headerLocator");
  }
}
