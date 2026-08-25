package dev.quokkify.test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import dev.quokkify.elements.table.model.ColumnAssertions;
import dev.quokkify.elements.table.model.RowAssertions;
import dev.quokkify.elements.table.model.SelenideTableQuery;
import dev.quokkify.elements.table.model.TableAssertions;
import dev.quokkify.elements.table.model.TableCellRef;
import dev.quokkify.elements.table.model.TableColumnRef;
import dev.quokkify.elements.table.model.TableDomAdapters;
import dev.quokkify.elements.table.model.TableQueryRow;
import dev.quokkify.elements.table.model.TypedTableCellRef;
import dev.quokkify.elements.table.model.UnsupportedTableEditException;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TableAssertionsActionsContractTest extends BaseTest {

  private static final Column NAME = new Column("Name");
  private static final Column STATUS = new Column("Status");
  private static final Column ACTION = new Column("Action");
  private static final Column INPUT = new Column("Input");
  private static final Column CHECK = new Column("Check");
  private static final Column RADIO = new Column("Radio");
  private static final Column SELECT = new Column("Select");
  private static final Column READ_ONLY = new Column("Read only");
  private static final Column LINK = new Column("Link");

  private SelenideTableQuery<Column> table;

  @BeforeMethod
  public void openFixture() {
    String baseUrl = System.getenv().getOrDefault("NGINX_BASE_URL", "http://localhost");
    Selenide.open(baseUrl + "/table-model-contract/");
    table = SelenideTableQuery.of(Selenide.$("#assertion-actions"),
        TableDomAdapters.classic(), Column::header);
  }

  @Test(description = "Table assertions use native waiting for delayed headers and rows")
  public void waitsForTableStateWithOneRootCondition() {
    Selenide.executeJavaScript("window.prepareDelayedAssertionHeader()");
    table.shouldHave(TableAssertions.headers(
        "Name", "Status", "Action", "Input", "Check", "Radio", "Select", "Read only", "Link"),
        Duration.ofSeconds(2));

    Selenide.executeJavaScript("window.prepareDelayedAssertionRow()");
    table.shouldHave(TableAssertions.rowCount(2), Duration.ofSeconds(2))
        .shouldHave(TableAssertions.columnExists(STATUS))
        .shouldHave(TableAssertions.matchingRow(RowAssertions.cell(NAME, Condition.text("Beta"))));
  }

  @Test(description = "Row and cell assertions wait natively for delayed cell state")
  public void waitsForRowAndCellState() {
    Selenide.executeJavaScript("window.prepareDelayedAssertionCell()");

    table.row(0).shouldHave(RowAssertions.cell(STATUS, Condition.exactText("Ready")),
        Duration.ofSeconds(2));
    table.row(1).shouldHave(RowAssertions.values(
        "Beta", "Ready", "", "", "", "", "", "fixed too", ""));
    table.row(0).requiredCell(STATUS).shouldHave(Condition.exactText("Ready"));
  }

  @Test(description = "Row, cell, and ordered duplicate column handles survive table remount")
  public void reResolvesEveryHandleAfterRemount() {
    TableQueryRow<Column> row = table.row(0);
    TableCellRef<Column> cell = row.requiredCell(STATUS);
    TableColumnRef<Column> column = table.column(STATUS);

    Selenide.executeJavaScript("window.remountAssertionTable()");

    row.shouldHave(RowAssertions.cell(NAME, Condition.exactText("Alpha")));
    cell.shouldBe(Condition.visible).shouldHave(Condition.exactText("Ready"));
    column.shouldHave(ColumnAssertions.values("Ready", "Ready"));
  }

  @Test(description = "Button and link capabilities resolve and act on embedded controls")
  public void actsOnButtonsAndLinks() {
    table.row(0).requiredCell(ACTION).button().shouldBe(Condition.enabled).click();
    Selenide.$("#table-action-result").shouldHave(Condition.exactText("button"));

    table.row(0).requiredCell(LINK).link().shouldHave(Condition.text("Details")).click();
    Selenide.$("#table-action-result").shouldHave(Condition.exactText("link"));
  }

  @Test(description = "Input, checkbox, radio, and select use distinct typed capabilities")
  public void editsExplicitEmbeddedControls() {
    table.row(0).requiredCell(INPUT).input().setValue("changed");
    Assertions.assertThat(table.row(0).requiredCell(INPUT).input().value()).isEqualTo("changed");

    table.row(0).requiredCell(CHECK).checkbox().setSelected(true);
    Assertions.assertThat(table.row(0).requiredCell(CHECK).checkbox().isSelected()).isTrue();

    table.row(0).requiredCell(RADIO).radio().select();
    Assertions.assertThat(table.row(0).requiredCell(RADIO).radio().isSelected()).isTrue();
    Assertions.assertThatThrownBy(() -> table.row(0).requiredCell(RADIO).radio().setSelected(false))
        .isInstanceOf(UnsupportedTableEditException.class);

    table.row(0).requiredCell(SELECT).select().selectOption("Two");
    Assertions.assertThat(table.row(0).requiredCell(SELECT).select().selectedText()).isEqualTo("Two");
  }

  @Test(description = "Contenteditable uses inherited HTML semantics and text editing")
  public void editsContenteditableCells() {
    SelenideTableQuery<String> contenteditable = SelenideTableQuery.of(
        Selenide.$("#contenteditable-table"), TableDomAdapters.classic(), header -> header);

    Assertions.assertThat(contenteditable.row(0).requiredCell("Direct").editable().value())
        .isEqualTo("Direct");
    Assertions.assertThat(contenteditable.row(0).requiredCell("Inherited").editable().value())
        .isEqualTo("Inherited");
    Assertions.assertThat(contenteditable.row(0).requiredCell("Empty").editable().value())
        .isEqualTo("Empty");
    Assertions.assertThat(contenteditable.row(0).requiredCell("Plaintext").editable().value())
        .isEqualTo("Plaintext");
    contenteditable.row(0).requiredCell("Direct").editable().setValue("Changed");
    Assertions.assertThat(contenteditable.row(0).requiredCell("Direct").editable().value())
        .isEqualTo("Changed");
    Assertions.assertThatThrownBy(() -> contenteditable.row(0).requiredCell("False").editable())
        .isInstanceOf(UnsupportedTableEditException.class);
  }

  @Test(description = "Read-only cells reject the explicit edit capability")
  public void rejectsReadOnlyEditingExplicitly() {
    Assertions.assertThatThrownBy(() -> table.row(0).requiredCell(READ_ONLY).editable())
        .isInstanceOf(UnsupportedTableEditException.class)
        .hasMessageContaining("row index 0")
        .hasMessageContaining("Read only");
  }

  @Test(description = "Assertion errors identify table, row, typed column, and actual values")
  public void reportsDiagnosticAddressesAndValues() {
    Assertions.assertThatThrownBy(() -> table.row(0).shouldHave(
            RowAssertions.values("wrong"), Duration.ofMillis(50)))
        .hasMessageContaining("#assertion-actions")
        .hasMessageContaining("row index 0")
        .hasMessageContaining("headers=[Name, Status")
        .hasMessageContaining("Alpha, Ready");

    Assertions.assertThatThrownBy(() -> table.column(STATUS).shouldHave(
            ColumnAssertions.values("wrong"), Duration.ofMillis(50)))
        .hasMessageContaining("column key=" + STATUS)
        .hasMessageContaining("header=Status")
        .hasMessageContaining("Alpha, Ready");
  }

  @Test(description = "Condition cell text stays on its captured row snapshot")
  public void keepsIndexedAndTypedConditionReadsOnCapturedSnapshot() {
    AtomicBoolean redirected = new AtomicBoolean();
    AtomicReference<String> indexedText = new AtomicReference<>();
    AtomicReference<String> typedText = new AtomicReference<>();
    SelenideTableQuery<Column> query = SelenideTableQuery.of(Selenide.$("#query-classic"),
        TableDomAdapters.classic(), Column::header);

    query.requiredRow(row -> {
      TableCellRef<Column> indexed = row.cell(0);
      TypedTableCellRef<Column> typed = row.requiredCell(new Column("Country"));
      if (redirected.compareAndSet(false, true)) {
        Selenide.executeJavaScript("window.redirectQueryClassicRoot()");
        indexedText.set(indexed.text());
        typedText.set(typed.text());
      }
      return true;
    }, Duration.ofSeconds(2));

    Assertions.assertThat(indexedText).hasValue("Austria");
    Assertions.assertThat(typedText).hasValue("Austria");
    Assertions.assertThat(query.row(0).cell(0).text()).isEqualTo("Changed");
  }

  private record Column(String header) {
  }
}
