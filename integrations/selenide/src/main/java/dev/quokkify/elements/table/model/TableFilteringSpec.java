package dev.quokkify.elements.table.model;

import java.util.Objects;
import java.util.function.Function;

import com.codeborne.selenide.SelenideElement;

/** Selenide resolvers required to compose the filtering capability around a table query. */
public record TableFilteringSpec<C>(Function<C, SelenideElement> control, TableStateToken stateToken) {

  public TableFilteringSpec {
    control = Objects.requireNonNull(control, "control");
    stateToken = Objects.requireNonNull(stateToken, "stateToken");
  }
}
