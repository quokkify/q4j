package dev.quokkify.elements.table.classic;

import java.util.function.Function;

import dev.quokkify.elements.table.classic.base.BaseClassicTable;

/**
 * Table UI element and methods of working with it.
 *
 * @param <T> enum with columns enumerations
 */
public class Table<T extends Enum<T>> extends BaseClassicTable<T> {

  /**
   * Fetch column index by column index in the UI table.
   */
  @Override
  protected Function<T, Integer> fetchColumnIndex() {
    return Enum::ordinal;
  }
}
