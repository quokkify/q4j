package dev.quokkify.elements.table.model;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.codeborne.selenide.SelenideElement;

/** Selenide resolvers required to compose the pagination capability around a table query. */
public record TablePaginationSpec(Supplier<SelenideElement> nextControl,
                                  Supplier<SelenideElement> previousControl,
                                  BooleanSupplier canNext,
                                  BooleanSupplier canPrevious,
                                  Supplier<Optional<TablePageMetadata>> metadata,
                                  TableStateToken stateToken) {

  public TablePaginationSpec {
    nextControl = Objects.requireNonNull(nextControl, "nextControl");
    previousControl = Objects.requireNonNull(previousControl, "previousControl");
    canNext = Objects.requireNonNull(canNext, "canNext");
    canPrevious = Objects.requireNonNull(canPrevious, "canPrevious");
    metadata = Objects.requireNonNull(metadata, "metadata");
    stateToken = Objects.requireNonNull(stateToken, "stateToken");
  }

  public TablePaginationSpec(Supplier<SelenideElement> nextControl,
                             Supplier<SelenideElement> previousControl,
                             BooleanSupplier canNext,
                             BooleanSupplier canPrevious,
                             TableStateToken stateToken) {
    this(nextControl, previousControl, canNext, canPrevious, Optional::<TablePageMetadata>empty,
        stateToken);
  }
}
