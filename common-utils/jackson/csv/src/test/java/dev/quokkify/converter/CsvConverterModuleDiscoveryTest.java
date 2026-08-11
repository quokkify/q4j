package dev.quokkify.converter;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.converter.support.CsvCustomFormatTestModule.PetRow;
import dev.quokkify.converter.support.CsvCustomFormatTestModule.PetType;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class CsvConverterModuleDiscoveryTest {

  @TmsLink("CSV_CONVERTER_MODULE_DISCOVERY_ID_1")
  @TestGroup("Csv")
  @Test(description = "Verify CSV serialization keeps default enum format when module discovery is disabled")
  public void shouldSerializeWithoutCustomFormattingWhenModuleNotRegistered() throws Exception {
    CsvMapper mapper = CsvConverter.createCsvMapper(false, false);

    String csv = writeRows(mapper, List.of(new PetRow("Tom", PetType.CAT)), PetRow.class);

    Assertions.assertThat(csv).contains("Tom,CAT");
  }

  @TmsLink("CSV_CONVERTER_MODULE_DISCOVERY_ID_2")
  @TestGroup("Csv")
  @Test(description = "Verify CSV serialization applies discovered custom module formatting")
  public void shouldSerializeWithCustomFormattingWhenModuleRegistered() {
    String csv = CsvConverter.toCsv(List.of(new PetRow("Tom", PetType.CAT)), PetRow.class);

    Assertions.assertThat(csv).contains("Tom,cat");
  }

  @TmsLink("CSV_CONVERTER_MODULE_DISCOVERY_ID_3")
  @TestGroup("Csv")
  @Test(description = "Verify CSV deserialization fails without discovered custom module")
  public void shouldFailDeserializeCustomFormattedEnumWhenModuleNotRegistered() {
    CsvMapper mapper = CsvConverter.createCsvMapper(false, false);
    String csv = "name,type\nTom,cat\n";

    Assertions.assertThatThrownBy(() -> readRows(mapper, csv, PetRow.class))
        .isInstanceOf(Exception.class);
  }

  @TmsLink("CSV_CONVERTER_MODULE_DISCOVERY_ID_4")
  @TestGroup("Csv")
  @Test(description = "Verify CSV deserialization succeeds with discovered custom module")
  public void shouldDeserializeCustomFormattedEnumWhenModuleRegistered() {
    String csv = "name,type\nTom,cat\n";

    List<PetRow> rows = CsvConverter.fromString(csv, PetRow.class);

    Assertions.assertThat(rows).containsExactly(new PetRow("Tom", PetType.CAT));
  }

  @TmsLink("CSV_CONVERTER_MODULE_DISCOVERY_ID_5")
  @TestGroup("Csv")
  @Test(description = "Verify default discovered modules support JDK8 Optional serialization and deserialization for CSV")
  public void shouldUseDefaultDiscoveredJdk8Module() {
    OptionalRow expected = new OptionalRow(Optional.of("Tom"));

    String csv = CsvConverter.toCsv(List.of(expected), OptionalRow.class);
    List<OptionalRow> actual = CsvConverter.fromString(csv, OptionalRow.class);

    Assertions.assertThat(csv).contains("Tom");
    Assertions.assertThat(actual).containsExactly(expected);
  }

  private static <T> String writeRows(CsvMapper mapper, List<T> rows, Class<T> type) throws Exception {
    CsvSchema schema = mapper.schemaFor(type).withHeader();
    ObjectWriter writer = mapper.writerFor(type).with(schema);
    StringWriter sw = new StringWriter();
    try (var seq = writer.writeValues(sw)) {
      for (T row : rows) {
        seq.write(row);
      }
    }
    return sw.toString();
  }

  private static <T> List<T> readRows(CsvMapper mapper, String csv, Class<T> type) throws Exception {
    CsvSchema schema = CsvSchema.emptySchema().withHeader();
    ObjectReader reader = mapper.readerFor(type).with(schema);
    return reader.<T>readValues(new StringReader(csv)).readAll();
  }

  record OptionalRow(Optional<String> value) {
  }
}
