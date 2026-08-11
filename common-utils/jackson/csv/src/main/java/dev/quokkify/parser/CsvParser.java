package dev.quokkify.parser;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.quokkify.converter.CsvConverter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * High-level CSV parser built on top of Jackson CSV.
 *
 * <p>Core API is schema-first to avoid duplication. Convenience overloads are kept (deprecated)
 * and delegate to the core methods with CsvSchemas.header()/noHeader(...).</p>
 */
public final class CsvParser {

  private CsvParser() {
  }

  /*
    =========================
    CORE (schema-first)
    =========================
    */

  /**
   * Reads all rows as String[] using a custom schema (header/no-header, separators, etc.).
   */
  public static List<String[]> readAllLines(File file, CsvSchema schema) {
    Objects.requireNonNull(file, "file must not be null");
    Objects.requireNonNull(schema, "schema must not be null");
    ensureExists(file);
    return CsvConverter.readRows(file, schema);
  }

  /**
   * Reads all rows as maps using a header-based schema.
   */
  public static List<Map<String, String>> readLinesDataHeaderAware(File file, CsvSchema headerSchema) {
    Objects.requireNonNull(file, "file must not be null");
    Objects.requireNonNull(headerSchema, "headerSchema must not be null");
    ensureExists(file);
    return CsvConverter.mapsFromFile(file, headerSchema);
  }

  /**
   * Parses CSV into beans/records using a custom schema.
   */
  public static <T> List<T> parse(File file, Class<T> cls, CsvSchema schema) {
    Objects.requireNonNull(file, "file must not be null");
    Objects.requireNonNull(cls, "cls must not be null");
    Objects.requireNonNull(schema, "schema must not be null");
    ensureExists(file);
    return CsvConverter.readBeans(file, cls, schema);
  }

  /**
   * Parses CSV into an arbitrary generic target using a custom schema.
   */
  public static <T> T parse(File file, TypeReference<T> typeRef, CsvSchema schema) {
    Objects.requireNonNull(file, "file must not be null");
    Objects.requireNonNull(typeRef, "typeRef must not be null");
    Objects.requireNonNull(schema, "schema must not be null");
    ensureExists(file);
    return CsvConverter.readBeans(file, typeRef, schema);
  }

  /**
   * Writes beans/records using a custom schema.
   */
  public static <T> File write(File csvFile, List<T> rows, Class<T> cls, CsvSchema schema) {
    Objects.requireNonNull(csvFile, "csvFile must not be null");
    Objects.requireNonNull(rows, "rows must not be null");
    Objects.requireNonNull(cls, "cls must not be null");
    Objects.requireNonNull(schema, "schema must not be null");
    if (rows.isEmpty()) throw new RuntimeException("rows must not be empty");
    return CsvConverter.toCsvFile(csvFile, rows, cls, schema);
  }

  /**
   * Writes {@code List<Map>} using a custom (usually header-based) schema.
   */
  public static File write(File csvFile, List<Map<String, ?>> rows, CsvSchema schema) {
    Objects.requireNonNull(csvFile, "csvFile must not be null");
    Objects.requireNonNull(rows, "rows must not be null");
    Objects.requireNonNull(schema, "schema must not be null");
    if (rows.isEmpty()) throw new RuntimeException("rows must not be empty");
    return CsvConverter.toCsvFileFromMaps(csvFile, rows, schema);
  }

  /* ---------- helpers ---------- */

  private static void ensureExists(File file) {
    if (!file.exists()) {
      throw new IllegalArgumentException("CSV file does not exist: " + file.getAbsolutePath());
    }
  }
}
