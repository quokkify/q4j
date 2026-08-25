package dev.quokkify.elements.table.model;

/** Caller-provided observable token that changes whenever the rendered table state changes. */
@FunctionalInterface
public interface TableStateToken {

  String current();
}
