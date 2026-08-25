package dev.quokkify.elements.table.model;

/** Header strategy for a table whose cells are not addressable by displayed headers. */
public final class NoTableHeaders implements TableHeaderLocator {

  private static final NoTableHeaders INSTANCE = new NoTableHeaders();

  private NoTableHeaders() {
  }

  /** Returns the shared immutable strategy instance. */
  public static NoTableHeaders instance() {
    return INSTANCE;
  }
}
