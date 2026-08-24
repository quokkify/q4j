package dev.quokkify.elements.table.model;

import com.codeborne.selenide.Driver;

/** One Selenide-polled assertion evaluated against a single current table snapshot. */
public interface TableAssertion<C> {

  String description();

  boolean test(Driver driver, TableAssertionContext<C> table);
}
