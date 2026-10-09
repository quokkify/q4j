package dev.quokkify.elements.table;

import java.util.Objects;
import java.util.Optional;

import org.openqa.selenium.By;

public record TableLayout(By rows, By cells, By headers) {

  private static final String XPATH_PREFIX = "By.xpath: ";

  public TableLayout {
    Objects.requireNonNull(rows, "rows");
    Objects.requireNonNull(cells, "cells");
    Objects.requireNonNull(headers, "headers");
  }

  /**
   * Native table markup: data rows from {@code <tbody>}, headers from the last {@code <thead>} row ({@code th} or
   * {@code td}). Grouped headers work only when that last row lists every leaf column.
   */
  public static TableLayout html() {
    return new TableLayout(
        By.xpath("./tbody/tr[td]"),
        By.xpath("./*[self::td or self::th]"),
        By.xpath("./thead/tr[last()]/*[self::th or self::td]"));
  }

  public static TableLayout aria() {
    return new TableLayout(
        By.xpath(".//*[@role='row'][*[@role='cell' or @role='gridcell']]"),
        By.xpath("./*[@role='cell' or @role='gridcell' or @role='rowheader']"),
        By.xpath(".//*[@role='columnheader']"));
  }

  public static TableLayout of(By rows, By cells, By headers) {
    return new TableLayout(rows, cells, headers);
  }

  static Optional<String> xpath(By by) {
    String description = by.toString();
    if (description.startsWith(XPATH_PREFIX)) {
      return Optional.of(description.substring(XPATH_PREFIX.length()));
    }
    return Optional.empty();
  }
}
