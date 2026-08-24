package dev.quokkify.elements.table.model;

import java.util.List;

/** Condition evaluated against current column values in DOM order. */
public interface ColumnAssertion {

  String description();

  boolean test(List<String> actualValues);
}
