package dev.quokkify.elements.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TableSnapshotKeyedTest {

  private static final String TABLE = "#t";
  private static final List<String> HEADERS = List.of("Name", "Properties");

  @Test
  public void keysTextsAndItemsByAnotherColumnInRowOrder() {
    TableSnapshot snap = parse(row(List.of("Beta", "Green"), List.of(List.of(), List.of("Green"))),
        row(List.of("Alpha", "Red Blue"), List.of(List.of(), List.of("Red", "Blue"))));

    assertThat(snap.columnBy("Name", "Properties")).containsExactly(
        Map.entry("Beta", "Green"), Map.entry("Alpha", "Red Blue"));
    assertThat(snap.columnValuesBy("Name", "Properties")).containsExactly(
        Map.entry("Beta", List.of("Green")), Map.entry("Alpha", List.of("Red", "Blue")));
  }

  @Test
  public void shortRowKeepsItsKeyWithAnEmptyValue() {
    TableSnapshot snap = parse(row(List.of("Alpha"), List.of(List.of())));

    assertThat(snap.columnBy("Name", "Properties")).containsExactly(Map.entry("Alpha", ""));
    assertThat(snap.columnValuesBy("Name", "Properties")).containsExactly(Map.entry("Alpha", List.of()));
  }

  @Test
  public void duplicateKeyFailsNamingBothRows() {
    TableSnapshot snap = parse(row(List.of("Alpha", "a"), null), row(List.of("Beta", "b"), null),
        row(List.of("Alpha", "c"), null));

    assertThatThrownBy(() -> snap.columnBy("Name", "Properties"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("rows 0 and 2")
        .hasMessageContaining("\"Alpha\"")
        .hasMessageContaining(TABLE);
  }

  @Test
  public void rowWithoutKeyCellFails() {
    TableSnapshot snap = parse(row(List.of("Alpha", "a"), null), row(List.of("only"), null));

    assertThatThrownBy(() -> snap.columnBy("Properties", "Name"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("row 1")
        .hasMessageContaining("\"Properties\"");
  }

  @Test
  public void missingHeaderIsNotFound() {
    TableSnapshot snap = parse(row(List.of("Alpha", "a"), null));

    assertThatThrownBy(() -> snap.columnBy("Id", "Name")).isInstanceOf(TableColumnException.class)
        .hasMessageContaining("\"Id\" not found");
    assertThatThrownBy(() -> snap.columnValuesBy("Name", "Chips")).isInstanceOf(TableColumnException.class)
        .hasMessageContaining("\"Chips\" not found");
  }

  @Test
  public void resultIsUnmodifiable() {
    TableSnapshot snap = parse(row(List.of("Alpha", "a"), null));

    assertThatThrownBy(() -> snap.columnBy("Name", "Properties").put("x", "y"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @SafeVarargs
  private static TableSnapshot parse(Map<String, Object>... rows) {
    Map<String, Object> result = new HashMap<>();
    result.put("headers", HEADERS);
    result.put("rows", List.of(rows));
    return TableSnapshot.fromScript(result, TABLE);
  }

  private static Map<String, Object> row(List<String> texts, List<List<String>> values) {
    Map<String, Object> row = new HashMap<>();
    row.put("texts", texts);
    row.put("values", values);
    return row;
  }
}
