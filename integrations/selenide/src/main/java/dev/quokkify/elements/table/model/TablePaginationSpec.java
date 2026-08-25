package dev.quokkify.elements.table.model;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.codeborne.selenide.SelenideElement;

/** Selenide resolvers required to compose the pagination capability around a table query. */
public record TablePaginationSpec(Supplier<SelenideElement> nextControl,
                                  Supplier<SelenideElement> previousControl,
                                  Supplier<Optional<TablePageMetadata>> metadata,
                                  TableStateToken stateToken) {

  public TablePaginationSpec {
    nextControl = Objects.requireNonNull(nextControl, "nextControl");
    previousControl = Objects.requireNonNull(previousControl, "previousControl");
    metadata = Objects.requireNonNull(metadata, "metadata");
    stateToken = Objects.requireNonNull(stateToken, "stateToken");
  }

  public TablePaginationSpec(Supplier<SelenideElement> nextControl,
                             Supplier<SelenideElement> previousControl,
                             TableStateToken stateToken) {
    this(nextControl, previousControl, Optional::<TablePageMetadata>empty, stateToken);
  }
}
