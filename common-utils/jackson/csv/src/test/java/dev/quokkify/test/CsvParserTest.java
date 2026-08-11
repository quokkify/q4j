package dev.quokkify.test;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.model.CsvHeaderEnum;
import dev.quokkify.model.CsvSchemas;
import dev.quokkify.parser.CsvParser;
import dev.quokkify.util.FileUtils;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

/**
 * Tests for CSV parsing/serialization based on Jackson CSV stack:
 * - CsvHeaderEnum (enum-driven columns)
 * - CsvSchemas     (schema factory)
 * - CsvConverter   (low-level CSV I/O)
 * - CsvParser      (high-level API)
 */
public class CsvParserTest {

  private static final File FILE_WITH_HEADER = new File(FileUtils.getResourceAsFilePath("test_file.csv"));
  private static final File FILE_NO_HEADER = new File(FileUtils.getResourceAsFilePath("test_file_no_header.csv"));

  @TmsLink("CSV_PARSER_ID_1")
  @TestGroup("Csv")
  @Test(description = "Verify parse CSV file with header to record model")
  public void testParseCsvFileToObjectHeader() {
    List<User> expected = List.of(new User("John", 18), new User("Kate", 99));
    CsvSchema schema = User.Column.headerSchema();
    List<User> actual = CsvParser.parse(FILE_WITH_HEADER, User.class, schema);
    Assertions.assertThat(actual)
        .as("Loaded users are incorrect")
        .isEqualTo(expected);
  }

  @TmsLink("CSV_PARSER_ID_2")
  @TestGroup("Csv")
  @Test(description = "Verify read all lines as String[] with header")
  public void testReadAllLinesHeader() {
    List<String[]> expectedLines = new LinkedList<>();
    expectedLines.add(new String[] {"name", "age"});
    expectedLines.add(new String[] {"John", "18"});
    expectedLines.add(new String[] {"Kate", "99"});
    List<String[]> actual = CsvParser.readAllLines(FILE_WITH_HEADER, CsvSchemas.header());
    Assertions.assertThat(actual)
        .as("Loaded rows are incorrect")
        .hasSameElementsAs(expectedLines);
  }

  @TmsLink("CSV_PARSER_ID_3")
  @TestGroup("Csv")
  @Test(description = "Verify read all rows as Map<String,String> using header mapping")
  public void testReadWithHeaderMapping() {
    List<Map<String, String>> expected = List.of(
        Map.of("name", "John", "age", "18"),
        Map.of("name", "Kate", "age", "99")
    );
    List<Map<String, String>> actual = CsvParser.readLinesDataHeaderAware(FILE_WITH_HEADER, CsvSchemas.header());
    Assertions.assertThat(actual)
        .as("Header-aware maps are incorrect")
        .hasSameElementsAs(expected);
  }

  @TmsLink("CSV_PARSER_ID_4")
  @TestGroup("Csv")
  @Test(description = "Verify writing records to CSV with header and compare content")
  public void testWriteBeansWithHeader() {
    List<User> rows = List.of(new User("John", 18), new User("Kate", 99));
    File out = FileUtils.createTempFile(FileUtils.FileExtension.CSV);
    CsvParser.write(out, rows, User.class, CsvSchemas.header());
    Assertions.assertThat(FileUtils.isFilesContentEquals(out, FILE_WITH_HEADER))
        .as("Written CSV content differs from the reference file with header")
        .isTrue();
  }

  @TmsLink("CSV_PARSER_ID_5")
  @TestGroup("Csv")
  @Test(description = "Verify parsing to generic target (List<User>) with header")
  public void testParseGenericTargetHeader() {
    List<User> expected = List.of(new User("John", 18), new User("Kate", 99));
    List<User> actual = CsvParser.parse(
        FILE_WITH_HEADER,
        new TypeReference<>() {
        },
        CsvSchemas.header()
    );
    Assertions.assertThat(actual)
        .as("Generic target parsing (List<User>) is incorrect")
        .isEqualTo(expected);
  }

  /* =========================
     No-header (positional) scenarios
     ========================= */

  @TmsLink("CSV_PARSER_ID_6")
  @TestGroup("Csv")
  @Test(description = "Verify parse CSV without header using enum-defined positional schema")
  public void testParseCsvFileToObjectNoHeaderPositional() {
    List<User> expected = List.of(
        new User("John", 18),
        new User("Kate", 99)
    );
    CsvSchema noHeaderSchema = User.Column.noHeaderSchema();
    List<User> actual = CsvParser.parse(FILE_NO_HEADER, User.class, noHeaderSchema);
    Assertions.assertThat(actual)
        .as("Positional parsing without header is incorrect")
        .isEqualTo(expected);
  }

  @TmsLink("CSV_PARSER_ID_7")
  @TestGroup("Csv")
  @Test(description = "Verify read all lines as String[] without header (positional)")
  public void testReadAllLinesNoHeaderPositional() {
    List<String[]> expected = new ArrayList<>();
    expected.add(new String[] {"John", "18"});
    expected.add(new String[] {"Kate", "99"});
    List<String[]> actual = CsvParser.readAllLines(FILE_NO_HEADER, CsvSchemas.rawNoHeader());
    Assertions.assertThat(actual)
        .as("Raw rows without header are incorrect")
        .hasSameElementsAs(expected);
  }

  @TmsLink("CSV_PARSER_ID_8")
  @TestGroup("Csv")
  @Test(description = "Verify writing Map rows using explicit header order from enum")
  public void testWriteMapsWithExplicitHeaderFromEnum() {
    List<Map<String, ?>> rows = List.of(
        Map.of("name", "John", "age", 18),
        Map.of("name", "Kate", "age", 99)
    );
    File out = FileUtils.createTempFile(FileUtils.FileExtension.CSV);
    CsvSchema headerSchema = User.Column.headerSchema();
    CsvParser.write(out, rows, headerSchema);
    Assertions.assertThat(FileUtils.isFilesContentEquals(out, FILE_WITH_HEADER))
        .as("Written Map-based CSV does not match reference with header")
        .isTrue();
  }

  /* =========================
     Models & enum (test-local)
     ========================= */

  /**
   * Record mapped by header names.
   */
  @JsonPropertyOrder({"name", "age"})
  public record User(String name, int age) {

    /**
     * Enum-driven columns for "users" CSV.
     * The order of constants defines the column order.
     */
    public enum Column implements CsvHeaderEnum {
      NAME, AGE;

      public static CsvSchema headerSchema() {
        return CsvHeaderEnum.headerSchema(Column.class);
      }

      public static CsvSchema noHeaderSchema() {
        return CsvHeaderEnum.noHeaderSchema(Column.class);
      }
    }
  }
}
