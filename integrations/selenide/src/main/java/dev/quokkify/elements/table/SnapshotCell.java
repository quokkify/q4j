package dev.quokkify.elements.table;

import java.util.List;
import java.util.Objects;

/**
 * A cell read by {@link Table#snapshot()}: its normalized text and the normalized texts of the items found inside it
 * by the locator passed to {@link Table#snapshot(org.openqa.selenium.By)} (empty when no locator was given).
 */
public record SnapshotCell(String text, List<String> values) {

  public SnapshotCell {
    Objects.requireNonNull(text, "text");
    values = List.copyOf(Objects.requireNonNull(values, "values"));
  }
}
