package dev.quokkify.elements.table;

import java.util.List;
import java.util.Objects;

import org.openqa.selenium.By;

/**
 * A locator in the form the snapshot script evaluates in the browser: an XPath or CSS expression.
 */
record SnapshotLocator(String kind, String expr) {

  private static final String XPATH_PREFIX = "By.xpath: ";
  private static final String CSS_PREFIX = "By.cssSelector: ";

  static SnapshotLocator of(By by) {
    String description = Objects.requireNonNull(by, "locator").toString();
    if (description.startsWith(XPATH_PREFIX)) {
      return new SnapshotLocator("xpath", description.substring(XPATH_PREFIX.length()));
    }
    if (description.startsWith(CSS_PREFIX)) {
      return new SnapshotLocator("css", description.substring(CSS_PREFIX.length()));
    }
    throw new UnsupportedOperationException("snapshot() supports By.xpath and By.cssSelector, got: " + description);
  }

  /** A list of strings, one of the argument types {@code JavascriptExecutor} documents: {@code [kind, expr]}. */
  List<String> toScriptArg() {
    return List.of(kind, expr);
  }
}
