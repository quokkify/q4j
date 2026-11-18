package dev.quokkify.elements.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codeborne.selenide.CheckResult;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CheckResult.Verdict.ACCEPT;
import static com.codeborne.selenide.CheckResult.Verdict.REJECT;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SnapshotConditionTest {

  private static final String TABLE = "#t";

  @Test
  public void acceptsWhenAssertionsPass() {
    CheckResult result = SnapshotCondition.evaluate(snapshot(),
        snap -> assertThat(snap.column("Name")).containsExactly("Alpha", "Beta"));

    assertThat(result.verdict()).isEqualTo(ACCEPT);
  }

  @Test
  public void rejectsWithAssertionMessageAsActualValue() {
    CheckResult result = SnapshotCondition.evaluate(snapshot(),
        snap -> assertThat(snap.column("Name")).as("names").containsExactly("Alpha"));

    assertThat(result.verdict()).isEqualTo(REJECT);
    assertThat(result.actualValue().toString()).contains("[names]").contains("Beta");
  }

  @Test
  public void rejectsMissingHeaderSoALateColumnIsRetried() {
    CheckResult result = SnapshotCondition.evaluate(snapshot(), snap -> snap.column("Region"));

    assertThat(result.verdict()).isEqualTo(REJECT);
    assertThat(result.actualValue().toString()).contains("TableColumnException").contains("\"Region\" not found")
        .contains("[Name, Properties]");
  }

  @Test
  public void rejectsRowThatIsNotRenderedYet() {
    CheckResult result = SnapshotCondition.evaluate(snapshot(), snap -> snap.rows().get(5));

    assertThat(result.verdict()).isEqualTo(REJECT);
    assertThat(result.actualValue().toString()).contains("IndexOutOfBoundsException");
  }

  @Test
  public void rejectsShortRowAndDuplicateKey() {
    assertThat(SnapshotCondition.evaluate(snapshot(), snap -> snap.rows().get(0).cell(5)).verdict()).isEqualTo(REJECT);
    assertThat(SnapshotCondition.evaluate(snapshot(), snap -> snap.columnBy("Properties", "Name")).verdict())
        .isEqualTo(REJECT);
  }

  @Test
  public void unexpectedExceptionIsNotTurnedIntoRejection() {
    assertThatThrownBy(() -> SnapshotCondition.evaluate(snapshot(), snap -> {
      throw new NullPointerException("bug in the test");
    })).isInstanceOf(NullPointerException.class).hasMessage("bug in the test");
  }

  @Test
  public void shouldSatisfyRejectsBadArgumentsBeforeBrowserCall() {
    Table byId = Table.of($("#t"), TableLayout.of(By.id("r"), By.xpath("./td"), By.xpath("./th")));
    Table html = Table.of($("#t"), TableLayout.html());

    assertThatThrownBy(() -> byId.shouldSatisfy(snap -> { }))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("By.id: r");
    assertThatThrownBy(() -> html.shouldSatisfy(By.name("chip"), snap -> { }))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("By.name: chip");
    assertThatThrownBy(() -> html.shouldSatisfy(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("assertions");
  }

  private static TableSnapshot snapshot() {
    Map<String, Object> result = new HashMap<>();
    result.put("headers", List.of("Name", "Properties"));
    result.put("rows", List.of(row("Alpha"), row("Beta")));
    return TableSnapshot.fromScript(result, TABLE);
  }

  private static Map<String, Object> row(String name) {
    Map<String, Object> row = new HashMap<>();
    row.put("texts", List.of(name, "x"));
    row.put("items", null);
    return row;
  }
}
