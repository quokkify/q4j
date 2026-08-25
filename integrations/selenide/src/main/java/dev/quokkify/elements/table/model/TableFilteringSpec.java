package dev.quokkify.elements.table.model;

import java.util.Objects;
import java.util.function.Function;

import com.codeborne.selenide.SelenideElement;

/** Selenide resolvers required to compose the filtering capability around a table query. */
public record TableFilteringSpec<F>(Function<F, SelenideElement> control,
                                    Function<F, java.util.Optional<String>> currentValue,
                                    TableStateToken stateToken) {

  public TableFilteringSpec {
    control = Objects.requireNonNull(control, "control");
    currentValue = Objects.requireNonNull(currentValue, "currentValue");
    stateToken = Objects.requireNonNull(stateToken, "stateToken");
  }

  public TableFilteringSpec(Function<F, SelenideElement> control, TableStateToken stateToken) {
    this(control, filter -> java.util.Optional.empty(), stateToken);
  }
}
