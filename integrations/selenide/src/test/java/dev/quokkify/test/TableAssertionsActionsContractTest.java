package dev.quokkify.test;

import java.time.Duration;

import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableLayout;
import dev.quokkify.elements.table.TableRow;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class TableAssertionsActionsContractTest extends BaseTest {

  private Table table;

  @BeforeMethod
  public void openFixture() {
    openAssertionsFixture();
    table = Table.of($("#assertion-actions"), TableLayout.html());
  }

  @Test(description = "Table assertions use native waiting for delayed headers and rows")
  public void waitsForTableStateWithOneRootCondition() {
    Selenide.executeJavaScript("window.prepareDelayedAssertionHeader()");
    table.headers().shouldHave(exactTexts(
        "Name", "Status", "Action", "Input", "Check", "Radio", "Select", "Read only", "Link"),
        Duration.ofSeconds(2));

    Selenide.executeJavaScript("window.prepareDelayedAssertionRow()");
    table.rows().shouldHave(size(2), Duration.ofSeconds(2));
    table.headers().shouldHave(itemWithText("Status"));
    table.row("Name", "Beta").self().should(exist);
  }

  @Test(description = "Row and cell assertions wait natively for delayed cell state")
  public void waitsForRowAndCellState() {
    Selenide.executeJavaScript("window.prepareDelayedAssertionCell()");

    table.row(0).cell("Status").shouldHave(exactText("Ready"), Duration.ofSeconds(2));
    table.row(1).cells().shouldHave(exactTexts(
        "Beta", "Ready", "", "", "", "", "", "fixed too", ""));
    table.row(0).cell("Status").shouldHave(exactText("Ready"));
  }

  @Test(description = "Row, cell, and ordered duplicate column handles survive table remount")
  public void reResolvesEveryHandleAfterRemount() {
    TableRow row = table.row(0);
    SelenideElement cell = row.cell("Status");
    ElementsCollection column = table.column("Status");

    Selenide.executeJavaScript("window.remountAssertionTable()");

    row.cell("Name").shouldHave(exactText("Alpha"));
    cell.shouldBe(visible).shouldHave(exactText("Ready"));
    column.shouldHave(exactTexts("Ready", "Ready"));
  }

  @Test(description = "Button and link capabilities resolve and act on embedded controls")
  public void actsOnButtonsAndLinks() {
    TableRow row = table.row(0);

    row.cell("Action").$("button").shouldBe(enabled).click();
    $("#table-action-result").shouldHave(exactText("button"));

    row.cell("Link").$("a").shouldHave(text("Details")).click();
    $("#table-action-result").shouldHave(exactText("link"));
  }

  @Test(description = "Input, checkbox, radio, and select use distinct typed capabilities")
  public void editsExplicitEmbeddedControls() {
    TableRow row = table.row(0);

    row.cell("Input").$("input").setValue("changed");
    row.cell("Input").$("input").shouldHave(value("changed"));

    row.cell("Check").$("input[type=checkbox]").setSelected(true);
    row.cell("Check").$("input[type=checkbox]").shouldBe(checked);

    row.cell("Radio").$("input[type=radio]").click();
    row.cell("Radio").$("input[type=radio]").shouldBe(selected);

    row.cell("Select").$("select").selectOption("Two");
    Assertions.assertThat(row.cell("Select").$("select").getSelectedOptionText()).isEqualTo("Two");
  }

  @Test(description = "Contenteditable uses inherited HTML semantics and text editing")
  public void editsContenteditableCells() {
    Table contenteditable = Table.of($("#contenteditable-table"), TableLayout.html());
    TableRow row = contenteditable.row(0);
    contenteditable.headers().shouldHave(exactTexts("Direct", "Inherited", "Empty", "Plaintext", "False"));

    row.cell("Direct").shouldHave(exactText("Direct"));
    row.cell("Inherited").shouldHave(exactText("Inherited"));
    row.cell("Empty").shouldHave(exactText("Empty"));
    row.cell("Plaintext").shouldHave(exactText("Plaintext"));
    row.cell("Direct").setValue("Changed");
    row.cell("Direct").shouldHave(exactText("Changed"));
    row.cell("False").shouldHave(attribute("contenteditable", "false"));
  }

  @Test(description = "Read-only cells reject the explicit edit capability")
  public void rejectsReadOnlyEditingExplicitly() {
    SelenideElement readOnly = table.row(0).cell("Read only");

    readOnly.shouldHave(exactText("fixed"));
    readOnly.$$("input, select, textarea, [contenteditable]").shouldHave(size(0));
  }

  @Test(description = "Assertion errors identify table, row, typed column, and actual values")
  public void reportsDiagnosticAddressesAndValues() {
    Assertions.assertThatThrownBy(() -> table.row(0).cells()
            .shouldHave(exactTexts("wrong"), Duration.ofMillis(50)))
        .hasMessageContaining("#assertion-actions")
        .hasMessageContaining("Alpha")
        .hasMessageContaining("Ready");

    Assertions.assertThatThrownBy(() -> table.column("Status")
            .shouldHave(exactTexts("wrong"), Duration.ofMillis(50)))
        .hasMessageContaining("#assertion-actions")
        .hasMessageContaining("Ready");
  }

  @Test(description = "Condition cell text stays on its captured row snapshot")
  public void keepsIndexedAndTypedConditionReadsOnCapturedSnapshot() {
    openQueriesFixture();
    Table query = Table.of($("#query-classic"), TableLayout.html());
    WebElement indexed = query.row(0).cell(0).toWebElement();
    WebElement typed = query.row(0).cell("Country").toWebElement();

    Selenide.executeJavaScript("window.redirectQueryClassicRoot()");

    Assertions.assertThat(indexed.getText()).isEqualTo("Austria");
    Assertions.assertThat(typed.getText()).isEqualTo("Austria");
    query.row(0).cell(0).shouldHave(exactText("Changed"));
  }
}
