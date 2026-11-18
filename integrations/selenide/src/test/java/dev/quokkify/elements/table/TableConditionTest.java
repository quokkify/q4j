package dev.quokkify.elements.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codeborne.selenide.CheckResult;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CheckResult.Verdict.ACCEPT;
import static com.codeborne.selenide.CheckResult.Verdict.REJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TableConditionTest {

  private static final String TABLE = "#t";
  private static final By CHIP = By.cssSelector(".chip");

  private final TableSnapshot chips = parse(List.of("Name", "Properties"),
      row(List.of("Alpha", "Red Blue"), List.of(List.of(), List.of("Red", "Blue"))),
      row(List.of("Beta", "Green"), List.of(List.of(), List.of("Green"))));

  @Test
  public void exactValuesByAcceptsSameMapInAnyRowOrder() {
    Map<String, List<String>> expected = new LinkedHashMap<>();
    expected.put("Beta", List.of("Green"));
    expected.put("Alpha", List.of("Red", "Blue"));

    CheckResult result = TableCondition.exactValuesBy("Name", "Properties", CHIP, expected).evaluate(chips);

    assertThat(result.verdict()).isEqualTo(ACCEPT);
  }

  @Test
  public void exactValuesByRejectsWithDifference() {
    CheckResult result = TableCondition.exactValuesBy("Name", "Properties", CHIP,
        Map.of("Alpha", List.of("Blue", "Red"), "Gamma", List.of())).evaluate(chips);

    assertThat(result.verdict()).isEqualTo(REJECT);
    assertThat(result.<String>getActualValue())
        .contains("2 rows, expected 2")
        .contains("missing (1):\n  Gamma")
        .contains("unexpected (1):\n  Beta")
        .contains("Alpha: expected [Blue, Red] but was [Red, Blue]");
  }

  @Test
  public void missingHeaderRejectsInsteadOfThrowing() {
    CheckResult result = TableCondition.exactValuesBy("Title", "Properties", CHIP, Map.of()).evaluate(chips);

    assertThat(result.verdict()).isEqualTo(REJECT);
    assertThat(result.<String>getActualValue()).contains("Title").contains("not found");
  }

  @Test
  public void ambiguousHeaderRejects() {
    TableSnapshot snap = parse(List.of("Name", "Name"), row(List.of("a", "b"), null));

    CheckResult result = TableCondition.exactRows(List.of("Name"), List.of(List.of("a"))).evaluate(snap);

    assertThat(result.verdict()).isEqualTo(REJECT);
    assertThat(result.<String>getActualValue()).contains("ambiguous");
  }

  @Test
  public void duplicateKeyRejects() {
    TableSnapshot snap = parse(List.of("Name", "Properties"),
        row(List.of("Alpha", "x"), null), row(List.of("Alpha", "y"), null));

    CheckResult result = TableCondition.exactValuesBy("Name", "Properties", CHIP, Map.of()).evaluate(snap);

    assertThat(result.verdict()).isEqualTo(REJECT);
    assertThat(result.<String>getActualValue()).contains("not unique").contains("rows 0 and 1");
  }

  @Test
  public void exactRowsComparesInRowOrderAndKeepsShortRowsAligned() {
    TableSnapshot snap = parse(List.of("Country", "Company"),
        row(List.of("Austria", "Alfreds"), null), row(List.of("Germany"), null));

    assertThat(TableCondition.exactRows(List.of("Company", "Country"),
        List.of(List.of("Alfreds", "Austria"), List.of("", "Germany"))).evaluate(snap).verdict()).isEqualTo(ACCEPT);
    CheckResult swapped = TableCondition.exactRows(List.of("Country"),
        List.of(List.of("Germany"), List.of("Austria"))).evaluate(snap);
    assertThat(swapped.verdict()).isEqualTo(REJECT);
    assertThat(swapped.<String>getActualValue()).contains("row 0: expected [Germany] but was [Austria]");
  }

  @Test
  public void exactRowsReportsMissingRows() {
    CheckResult result = TableCondition.exactRows(List.of("Name"),
        List.of(List.of("Alpha"), List.of("Beta"), List.of("Gamma"))).evaluate(chips);

    assertThat(result.<String>getActualValue()).contains("2 rows, expected 3").contains("missing (1):\n  row 2: [Gamma]");
  }

  @Test
  public void customConditionComparesProjection() {
    assertThat(TableCondition.of("row count", snap -> snap.rows().size(), 2).evaluate(chips).verdict())
        .isEqualTo(ACCEPT);
    assertThat(TableCondition.of("row count", snap -> snap.rows().size(), 3).evaluate(chips).verdict())
        .isEqualTo(REJECT);
  }

  @Test
  public void descriptionDoesNotPrintExpectedRows() {
    TableCondition condition = TableCondition.exactValuesBy("Name", "Properties", CHIP,
        Map.of("Alpha", List.of("Red", "Blue")));

    assertThat(condition).hasToString("\"Properties\" items by \"Name\" (1 rows)");
  }

  @Test
  public void unsupportedItemsLocatorFailsBeforeBrowser() {
    assertThatThrownBy(() -> TableCondition.exactValuesBy("Name", "Properties", By.id("chip"), Map.of()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void diffIsCappedForLargeTables() {
    Map<String, List<String>> expected = new HashMap<>();
    for (int i = 0; i < 25; i++) {
      expected.put("row" + i, List.of());
    }

    String diff = TableDiff.describe(expected, Map.of());

    assertThat(diff).contains("missing (25):").contains("... 15 more");
    assertThat(diff.lines()).hasSize(2 + TableDiff.LIMIT + 1);
  }

  @Test
  public void valuesByKeepsRowOrderAndIsUnmodifiable() {
    Map<String, List<String>> values = chips.valuesBy("Name", "Properties");

    assertThat(values.keySet()).containsExactly("Alpha", "Beta");
    assertThatThrownBy(() -> values.put("x", List.of())).isInstanceOf(UnsupportedOperationException.class);
  }

  @SafeVarargs
  private static TableSnapshot parse(List<String> headers, Map<String, Object>... rows) {
    Map<String, Object> result = new HashMap<>();
    result.put("headers", headers);
    result.put("rows", List.of(rows));
    return TableSnapshot.fromScript(result, TABLE);
  }

  private static Map<String, Object> row(List<String> texts, List<List<String>> values) {
    Map<String, Object> row = new HashMap<>();
    row.put("texts", new ArrayList<>(texts));
    row.put("values", values);
    return row;
  }
}
