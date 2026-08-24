package dev.quokkify.elements.table.model;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.codeborne.selenide.SelenideElement;

/** Selenide resolvers required to compose the sorting capability around a table query. */
public record TableSortingSpec<C>(Function<C, SelenideElement> control,
                                  Function<C, Optional<TableSortDirection>> currentDirection,
                                  TableStateToken stateToken) {

  public TableSortingSpec {
    control = Objects.requireNonNull(control, "control");
    currentDirection = Objects.requireNonNull(currentDirection, "currentDirection");
    stateToken = Objects.requireNonNull(stateToken, "stateToken");
  }

  public TableSortingSpec(Function<C, SelenideElement> control, TableStateToken stateToken) {
    this(control, column -> Optional.empty(), stateToken);
  }
}
