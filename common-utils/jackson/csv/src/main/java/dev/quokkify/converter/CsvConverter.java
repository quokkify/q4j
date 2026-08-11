package dev.quokkify.converter;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.quokkify.util.FileUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Utilities for CSV serialization and deserialization using Jackson {@link CsvMapper}.
 *
 * <p>Design:</p>
 * <ul>
 *   <li><b>Core (schema-first)</b>: methods that accept an explicit {@link CsvSchema} to avoid duplication.</li>
 *   <li><b>Convenience</b>: thin overloads that delegate to core with a default header-based schema.</li>
 *   <li>No checked exceptions are thrown; all are wrapped into {@link RuntimeException} with context.</li>
 *   <li>Configured with classpath module auto-discovery; supports POJO/record constructor binding
 *       (use {@code @JsonProperty} when column names differ).</li>
 * </ul>
 */
public final class CsvConverter {

  /**
   * Default mapper for CSV (trimming spaces, forgiving trailing columns).
   */
  private static final CsvMapper CSV = createCsvMapper(false, true);

  /**
   * Mapper for CSV that skips nulls during serialization.
   */
  private static final CsvMapper CSV_NON_NULL = createCsvMapper(true, true);

  private CsvConverter() {
  }

  static CsvMapper createCsvMapper(boolean ignoreNullFields, boolean registerDiscoveredModules) {
    CsvMapper.Builder builder = CsvMapper.builder()
        .enable(CsvParser.Feature.TRIM_SPACES)
        .enable(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE)
        .enable(CsvParser.Feature.WRAP_AS_ARRAY);

    if (ignoreNullFields) {
      builder.defaultPropertyInclusion(
          JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS));
    }

    CsvMapper mapper = builder.build();
    if (registerDiscoveredModules) {
      mapper.findAndRegisterModules();
    }
    return mapper;
  }

  /* =========================
     CORE — READ (schema-first)
     ========================= */

  /**
   * Reads rows as String[] using the provided schema (header/no-header, separators, etc.).
   */
  public static List<String[]> readRows(File file, CsvSchema schema) {
    try {
      if (schema.usesHeader()) {
        List<Map<String, String>> maps = mapsFromFile(file, schema);
        List<String[]> result = new ArrayList<>(maps.size() + 1);
        if (!maps.isEmpty()) {
          result.add(maps.get(0).keySet().toArray(new String[0]));
          for (Map<String, String> row : maps) {
            result.add(row.values().toArray(new String[0]));
          }
        }
        return result;
      } else {
        ObjectReader reader = CSV.readerFor(Object[].class).with(schema);
        List<String[]> out = new ArrayList<>();
        try (var it = reader.readValues(file)) {
          while (it.hasNext()) {
            Object[] row = (Object[]) it.next();
            String[] asStrings = new String[row.length];
            for (int i = 0; i < row.length; i++) {
              asStrings[i] = (row[i] == null) ? "" : String.valueOf(row[i]);
            }
            out.add(asStrings);
          }
        }
        return out;
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to read rows (schema) from CSV file: " + file.getAbsolutePath(), e);
    }
  }

  /**
   * Reads maps (column -> value) using a header-based schema.
   */
  @SuppressWarnings("unchecked")
  public static List<Map<String, String>> mapsFromFile(File file, CsvSchema headerSchema) {
    try {
      ObjectReader reader = CSV.readerFor(Map.class).with(headerSchema);
      return reader.<Map<String, String>>readValues(file).readAll();
    } catch (Exception e) {
      throw new RuntimeException("Failed to read maps (schema) from CSV file: " + file.getAbsolutePath(), e);
    }
  }

  /**
   * Reads beans/records using the provided schema.
   */
  public static <T> List<T> readBeans(File file, Class<T> type, CsvSchema schema) {
    try {
      ObjectReader reader = CSV.readerFor(type).with(schema);
      return reader.<T>readValues(file).readAll();
    } catch (Exception e) {
      throw new RuntimeException("Failed to read beans (schema) from CSV file -> "
          + type.getSimpleName() + ": " + file.getAbsolutePath(), e);
    }
  }

  /**
   * Reads an arbitrary generic target using the provided schema (e.g., {@code new TypeReference<List<User>>(){}}).
   */
  public static <T> T readBeans(File file, TypeReference<T> typeRef, CsvSchema schema) {
    try {
      JavaType javaType = TypeFactory.defaultInstance().constructType(typeRef);
      if (javaType.isCollectionLikeType()) {
        JavaType elemType = javaType.getContentType();
        ObjectReader elemReader = CSV.readerFor(elemType).with(schema);
        @SuppressWarnings("unchecked")
        T list = (T) elemReader.readValues(file).readAll();
        return list;
      }
      ObjectReader reader = CSV.readerFor(typeRef).with(schema);
      return reader.readValue(file);
    } catch (Exception e) {
      throw new RuntimeException("Failed to read generic target (schema) from CSV file: " + file.getAbsolutePath(), e);
    }
  }

  /* =========================
     CORE — WRITE (schema-first)
     ========================= */

  /**
   * Writes beans/records to a file using the provided schema.
   */
  public static <T> File toCsvFile(File csvFile, List<T> rows, Class<T> type, CsvSchema schema) {
    try {
      CsvSchema effective = normalizeWriteSchema(type, schema);
      ObjectWriter writer = CSV.writerFor(type).with(effective);
      try (var seq = writer.writeValues(csvFile)) {
        for (T row : rows) seq.write(row);
      }
      return csvFile;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize beans (schema) to CSV file -> "
          + type.getSimpleName() + ": " + csvFile.getAbsolutePath(), e);
    }
  }

  private static <T> CsvSchema normalizeWriteSchema(Class<T> type, CsvSchema given) {
    if (given.size() > 0) {
      return given;
    }
    CsvSchema schema = CSV.schemaFor(type)
        .withColumnSeparator(given.getColumnSeparator())
        .withArrayElementSeparator(given.getArrayElementSeparator())
        .withNullValue(given.getNullValueString());

    schema = applyHeader(schema, given);
    schema = applyQuote(schema, given);
    schema = applyEscape(schema, given);
    schema = applyComments(schema, given);

    if (given.skipsFirstDataRow()) schema = schema.withSkipFirstDataRow(true);
    if (given.strictHeaders()) schema = schema.withStrictHeaders(true);
    if (given.reordersColumns()) schema = schema.withColumnReordering(true);

    return schema;
  }

  private static CsvSchema applyHeader(CsvSchema schema, CsvSchema given) {
    return given.usesHeader() ? schema.withHeader() : schema.withoutHeader();
  }

  private static CsvSchema applyQuote(CsvSchema schema, CsvSchema given) {
    return given.usesQuoteChar() ? schema.withQuoteChar((char) given.getQuoteChar()) : schema.withoutQuoteChar();
  }

  private static CsvSchema applyEscape(CsvSchema schema, CsvSchema given) {
    return given.usesEscapeChar() ? schema.withEscapeChar((char) given.getEscapeChar()) : schema.withoutEscapeChar();
  }

  private static CsvSchema applyComments(CsvSchema schema, CsvSchema given) {
    return given.allowsComments() ? schema.withComments() : schema.withoutComments();
  }

  /**
   * Writes list of maps to a file using the provided (usually header-based) schema.
   */
  public static File toCsvFileFromMaps(File csvFile, List<Map<String, ?>> rows, CsvSchema schema) {
    try {
      if (rows == null || rows.isEmpty()) {
        throw new IllegalArgumentException("rows must not be null or empty");
      }
      ObjectWriter writer = CSV.writerFor(Map.class).with(schema);
      try (var seq = writer.writeValues(csvFile)) {
        for (Map<String, ?> row : rows) seq.write(row);
      }
      return csvFile;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize maps (schema) to CSV file: " + csvFile.getAbsolutePath(), e);
    }
  }

  /* =========================
     CONVENIENCE — READ (default header schema)
     ========================= */

  /**
   * Reads beans from a CSV string (expects header).
   */
  public static <T> List<T> fromString(String csv, Class<T> type) {
    try {
      CsvSchema schema = CsvSchema.emptySchema().withHeader();
      ObjectReader reader = CSV.readerFor(type).with(schema);
      return reader.<T>readValues(new StringReader(csv)).readAll();
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize CSV (String -> " + type.getSimpleName() + ")", e);
    }
  }

  /**
   * Reads a generic target from a CSV string (expects header).
   */
  public static <T> T fromString(String csv, TypeReference<T> typeRef) {
    try {
      CsvSchema schema = CsvSchema.emptySchema().withHeader();
      ObjectReader reader = CSV.readerFor(typeRef).with(schema);
      return reader.readValue(new StringReader(csv));
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize CSV (String -> TypeReference)", e);
    }
  }

  /**
   * Reads beans from a CSV file (expects header).
   */
  public static <T> List<T> fromFile(File file, Class<T> type) {
    return readBeans(file, type, CsvSchema.emptySchema().withHeader());
  }

  /**
   * Reads beans from a classpath resource (expects header).
   */
  public static <T> List<T> fromResource(String resourcePath, Class<T> type) {
    try (InputStream is = FileUtils.getNonNullResourceAsStream(resourcePath)) {
      CsvSchema schema = CsvSchema.emptySchema().withHeader();
      ObjectReader reader = CSV.readerFor(type).with(schema);
      return reader.<T>readValues(is).readAll();
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize CSV resource -> " + type.getSimpleName()
          + ": " + resourcePath, e);
    }
  }

  /**
   * Reads rows as String[]; set {@code withHeader=true} if the first line is a header.
   */
  public static List<String[]> rowsFromFile(File file, boolean withHeader) {
    CsvSchema schema = withHeader ? CsvSchema.emptySchema().withHeader() : CsvSchema.emptySchema();
    return readRows(file, schema);
  }

  /**
   * Reads maps (column -> value) using a default header-based schema.
   */
  public static List<Map<String, String>> mapsFromFile(File file) {
    return mapsFromFile(file, CsvSchema.emptySchema().withHeader());
  }

  /**
   * Reads a generic target (expects header).
   */
  public static <T> T readBeans(File file, TypeReference<T> typeRef) {
    return readBeans(file, typeRef, CsvSchema.emptySchema().withHeader());
  }

  /* =========================
     CONVENIENCE — WRITE (default header schema)
     ========================= */

  /**
   * Serializes beans to a CSV string with a header (schema derived from the type).
   */
  public static <T> String toCsv(List<T> rows, Class<T> type) {
    try {
      CsvSchema schema = CSV.schemaFor(type).withHeader();
      ObjectWriter writer = CSV.writerFor(type).with(schema);
      StringWriter sw = new StringWriter();
      try (var seq = writer.writeValues(sw)) {
        for (T row : rows) seq.write(row);
      }
      return sw.toString();
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize beans to CSV (String) -> " + type.getSimpleName(), e);
    }
  }

  /**
   * Serializes beans to a CSV string with a header, ignoring nulls.
   */
  public static <T> String toCsvIgnoreNulls(List<T> rows, Class<T> type) {
    try {
      CsvSchema schema = CSV_NON_NULL.schemaFor(type).withHeader();
      ObjectWriter writer = CSV_NON_NULL.writerFor(type).with(schema);
      StringWriter sw = new StringWriter();
      try (var seq = writer.writeValues(sw)) {
        for (T row : rows) seq.write(row);
      }
      return sw.toString();
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize beans to CSV (String, ignore nulls) -> " + type.getSimpleName(),
          e);
    }
  }

  /**
   * Writes beans to a CSV file with a header (schema derived from the type).
   */
  public static <T> File toCsvFile(File csvFile, List<T> rows, Class<T> type) {
    return toCsvFile(csvFile, rows, type, CSV.schemaFor(type).withHeader());
  }

  /**
   * Writes beans to a CSV file with a header, ignoring nulls.
   */
  public static <T> File toCsvFileIgnoreNulls(File csvFile, List<T> rows, Class<T> type) {
    try {
      CsvSchema schema = CSV_NON_NULL.schemaFor(type).withHeader();
      ObjectWriter writer = CSV_NON_NULL.writerFor(type).with(schema);
      try (var seq = writer.writeValues(csvFile)) {
        for (T row : rows) seq.write(row);
      }
      return csvFile;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize beans to CSV file (ignore nulls) -> "
          + type.getSimpleName() + ": " + csvFile.getAbsolutePath(), e);
    }
  }

  /**
   * Serializes a list of maps to a CSV string with a header derived from the first map's keys.
   * Column order follows the insertion order of the first map's keys.
   */
  public static String toCsvFromMaps(List<Map<String, ?>> rows) {
    try {
      if (rows == null || rows.isEmpty()) {
        throw new IllegalArgumentException("rows must not be null or empty");
      }
      CsvSchema.Builder b = CsvSchema.builder().setUseHeader(true);
      for (String col : rows.get(0).keySet()) b.addColumn(col);
      CsvSchema schema = b.build();

      ObjectWriter writer = CSV.writerFor(Map.class).with(schema);
      StringWriter sw = new StringWriter();
      try (var seq = writer.writeValues(sw)) {
        for (Map<String, ?> row : rows) seq.write(row);
      }
      return sw.toString();
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize maps to CSV (String)", e);
    }
  }

  /**
   * Writes a list of maps to a CSV file with a header derived from the first map's keys.
   */
  public static File toCsvFileFromMaps(File csvFile, List<Map<String, ?>> rows) {
    try {
      if (rows == null || rows.isEmpty()) {
        throw new IllegalArgumentException("rows must not be null or empty");
      }
      CsvSchema.Builder b = CsvSchema.builder().setUseHeader(true);
      for (String col : rows.get(0).keySet()) b.addColumn(col);
      CsvSchema schema = b.build();

      ObjectWriter writer = CSV.writerFor(Map.class).with(schema);
      try (var seq = writer.writeValues(csvFile)) {
        for (Map<String, ?> row : rows) seq.write(row);
      }
      return csvFile;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize maps to CSV file: " + csvFile.getAbsolutePath(), e);
    }
  }
}
