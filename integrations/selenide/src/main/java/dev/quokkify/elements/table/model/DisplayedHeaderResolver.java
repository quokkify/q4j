package dev.quokkify.elements.table.model;

import java.util.Objects;
import java.util.function.Function;

/**
 * Maps a typed column key to the text displayed by the DOM header cell.
 *
 * @param <C> typed column key
 */
@FunctionalInterface
public interface DisplayedHeaderResolver<C> {

  /**
   * Returns the exact displayed header text used for lookup.
   *
   * @param column typed column key
   * @return displayed header text
   */
  String displayedHeader(C column);

  /**
   * Creates a resolver from a function while rejecting null keys and values early.
   */
  static <C> DisplayedHeaderResolver<C> requiringNonNull(Function<C, String> resolver) {
    Objects.requireNonNull(resolver, "resolver");
    return column -> Objects.requireNonNull(resolver.apply(Objects.requireNonNull(column, "column")),
        "displayed header");
  }
}
