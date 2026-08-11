package dev.quokkify.model;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Contract for CSV column enums.
 * Each enum constant represents a CSV column in the desired order.
 * The {@link #title()} is the header text to appear in the CSV.
 */
public interface CsvHeaderEnum {

  /**
   * Display name of the column header (used in CSV header and schema).
   * Default: {@code enum.name().toLowerCase()}.
   */
  default String title() {
    return ((Enum<?>) this).name().toLowerCase();
  }

  /* ---------- static helpers ---------- */

  /**
   * Returns column titles in the enum declaration order.
   */
  static <E extends Enum<E> & CsvHeaderEnum> List<String> titles(Class<E> enumClass) {
    return Arrays.stream(enumClass.getEnumConstants()).map(CsvHeaderEnum::title).toList();
  }

  /**
   * Builds a header-based CsvSchema with columns taken from the enum titles, in order.
   */
  static <E extends Enum<E> & CsvHeaderEnum> CsvSchema headerSchema(Class<E> enumClass) {
    CsvSchema.Builder b = CsvSchema.builder().setUseHeader(true);
    for (String t : titles(enumClass)) b.addColumn(t);
    return b.build();
  }

  /**
   * Builds a no-header CsvSchema with positional columns defined by the enum titles.
   * Use together with @JsonPropertyOrder (or write/read as Map) to guarantee positional mapping.
   */
  static <E extends Enum<E> & CsvHeaderEnum> CsvSchema noHeaderSchema(Class<E> enumClass) {
    CsvSchema.Builder b = CsvSchema.builder(); // no header
    for (String t : titles(enumClass)) b.addColumn(t);
    return b.build();
  }

  /**
   * Header schema with a custom separator.
   */
  static <E extends Enum<E> & CsvHeaderEnum> CsvSchema headerSchema(Class<E> enumClass, char separator) {
    return headerSchema(enumClass).rebuild().setColumnSeparator(separator).build();
  }

  /**
   * No-header schema with a custom separator.
   */
  static <E extends Enum<E> & CsvHeaderEnum> CsvSchema noHeaderSchema(Class<E> enumClass, char separator) {
    return noHeaderSchema(enumClass).rebuild().setColumnSeparator(separator).build();
  }
}
