package dev.quokkify.model;

import java.util.List;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Factory class for building {@link CsvSchema} configurations for Jackson CSV.
 *
 * <p>Provides convenient methods for creating schemas with or without header,
 * using custom column separators, quote/escape characters, or explicit column order.</p>
 */
public final class CsvSchemas {

  private CsvSchemas() {
  }

  /**
   * Builds a header-based schema with custom separator, quote, and escape characters.
   */
  public static CsvSchema header(char separator, char quoteChar, char escapeChar) {
    return CsvSchema.emptySchema()
        .withHeader()
        .withColumnSeparator(separator)
        .withQuoteChar(quoteChar)
        .withEscapeChar(escapeChar);
  }

  /**
   * Builds a header-based schema with custom column separator.
   */
  public static CsvSchema header(char separator) {
    return CsvSchema.emptySchema()
        .withHeader()
        .withColumnSeparator(separator);
  }

  /**
   * Builds a default header-based schema (comma separator, double quote).
   */
  public static CsvSchema header() {
    return CsvSchema.emptySchema().withHeader();
  }

  /**
   * Builds a no-header schema with custom separator, quote, and escape characters.
   */
  public static CsvSchema noHeader(char separator, char quoteChar, char escapeChar) {
    return CsvSchema.emptySchema()
        .withColumnSeparator(separator)
        .withQuoteChar(quoteChar)
        .withEscapeChar(escapeChar);
  }

  /**
   * Builds a no-header schema with custom column separator.
   */
  public static CsvSchema noHeader(char separator) {
    return CsvSchema.emptySchema().withColumnSeparator(separator);
  }

  /**
   * Builds a default no-header schema (comma separator, double quote).
   */
  public static CsvSchema noHeader() {
    return CsvSchema.emptySchema();
  }

  /**
   * Builds a header-based schema with explicit column order and custom settings.
   *
   * @param columns    column names in the desired order
   * @param separator  column separator
   * @param quoteChar  quote character
   * @param escapeChar escape character
   */
  public static CsvSchema headerWithOrder(List<String> columns, char separator, char quoteChar, char escapeChar) {
    CsvSchema.Builder b = CsvSchema.builder().setUseHeader(true)
        .setColumnSeparator(separator)
        .setQuoteChar(quoteChar)
        .setEscapeChar(escapeChar);
    for (String c : columns) b.addColumn(c);
    return b.build();
  }

  /**
   * Builds a header-based schema with explicit column order and custom separator.
   */
  public static CsvSchema headerWithOrder(List<String> columns, char separator) {
    CsvSchema.Builder b = CsvSchema.builder().setUseHeader(true).setColumnSeparator(separator);
    for (String c : columns) b.addColumn(c);
    return b.build();
  }

  /**
   * Builds a header-based schema with explicit column order and default settings.
   */
  public static CsvSchema headerWithOrder(List<String> columns) {
    CsvSchema.Builder b = CsvSchema.builder().setUseHeader(true);
    for (String c : columns) b.addColumn(c);
    return b.build();
  }

  public static CsvSchema rawNoHeader() {
    return CsvSchema.emptySchema();
  }

  public static <E extends Enum<E> & CsvHeaderEnum> CsvSchema enumNoHeader(Class<E> enumClass) {
    return CsvHeaderEnum.noHeaderSchema(enumClass);
  }
}
