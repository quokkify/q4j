package dev.quokkify.elements.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TableSnapshotParsingTest {

  private static final String TABLE = "#t";

  @Test
  public void readsHeadersRowsAndCells() {
    TableSnapshot snap = parse(List.of("Country", "Company"),
        row(List.of("Austria", "Alfreds")), row(List.of("Germany", "Berglunds")));

    assertThat(snap.headers()).containsExactly("Country", "Company");
    assertThat(snap.rows().get(1).cell("Company").text()).isEqualTo("Berglunds");
    assertThat(snap.rows().get(0).cell(0).values()).isEmpty();
  }

  @Test
  public void normalizesHeadersAndTexts() {
    TableSnapshot snap = parse(List.of(" Company  Name \n"),
        row(List.of("  Alfreds \n Futterkiste "), List.of(List.of(" Chip A "))));

    assertThat(snap.column("Company Name")).containsExactly("Alfreds Futterkiste");
    assertThat(snap.columnValues("Company Name")).containsExactly(List.of("Chip A"));
  }

  @Test
  public void columnOfMissingHeaderIsNotFound() {
    TableSnapshot snap = parse(List.of("Country", "Company"), row(List.of("Austria", "Alfreds")));

    assertThatThrownBy(() -> snap.column("Region"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("not found")
        .hasMessageContaining(TABLE);
  }

  @Test
  public void columnOfRepeatedHeaderIsAmbiguous() {
    TableSnapshot snap = parse(List.of("Company", "Company"), row(List.of("Alfreds", "Duplicate")));

    assertThatThrownBy(() -> snap.column("Company"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("ambiguous");
  }

  @Test
  public void columnOnEmptySnapshotIsNotFound() {
    TableSnapshot snap = parse(List.of());

    assertThat(snap.rows()).isEmpty();
    assertThatThrownBy(() -> snap.column("X"))
        .isInstanceOf(TableColumnException.class)
        .hasMessageContaining("not found")
        .hasMessageContaining("[]");
  }

  @Test
  public void shortRowAlignsColumnButCellFails() {
    TableSnapshot snap = parse(List.of("Country", "Company"),
        row(List.of("Austria", "Alfreds")), row(List.of("Germany")));

    assertThat(snap.column("Company")).containsExactly("Alfreds", "");
    assertThat(snap.columnValues("Company")).containsExactly(List.of(), List.of());
    assertThatThrownBy(() -> snap.rows().get(1).cell("Company"))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("row 1")
        .hasMessageContaining("Company")
        .hasMessageContaining(TABLE);
    assertThatThrownBy(() -> snap.rows().get(1).cell(1))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("row 1");
  }

  @Test
  public void valuesNullMeansEmpty() {
    TableSnapshot snap = parse(List.of("Country", "Company"), row(List.of("Austria", "Alfreds")));

    assertThat(snap.rows().get(0).cells()).allSatisfy(cell -> assertThat(cell.values()).isEmpty());
  }

  @Test
  public void listsAreUnmodifiable() {
    TableSnapshot snap = parse(List.of("Country"), row(List.of("Austria"), List.of(List.of("chip"))));

    assertThatThrownBy(() -> snap.headers().add("x")).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> snap.rows().add(null)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> snap.rows().get(0).cells().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> snap.rows().get(0).cell(0).values().add("x"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void rowEqualityIgnoresHeaders() {
    List<SnapshotCell> cells = List.of(new SnapshotCell("Austria", List.of()));
    SnapshotRow first = new SnapshotRow(0, cells, List.of("Country"), TABLE);
    SnapshotRow second = new SnapshotRow(0, cells, List.of("Land"), "#other");

    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
  }

  @SafeVarargs
  private static TableSnapshot parse(List<String> headers, Map<String, Object>... rows) {
    Map<String, Object> result = new HashMap<>();
    result.put("headers", headers);
    result.put("rows", List.of(rows));
    return TableSnapshot.fromScript(result, TABLE);
  }

  private static Map<String, Object> row(List<String> texts) {
    return row(texts, null);
  }

  private static Map<String, Object> row(List<String> texts, List<List<String>> values) {
    Map<String, Object> row = new HashMap<>();
    row.put("texts", new ArrayList<>(texts));
    row.put("values", values);
    return row;
  }
}
