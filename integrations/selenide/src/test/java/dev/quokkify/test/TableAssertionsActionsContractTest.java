package dev.quokkify.test;

import java.time.Duration;

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
import static com.codeborne.selenide.Selectors.byRole;
import static com.codeborne.selenide.Selenide.$;

public class TableAssertionsActionsContractTest extends BaseTest {

  private SelenideElement table;

  @BeforeMethod
  public void openFixture() {
    openAssertionsFixture();
    table = $("#assertion-actions");
  }

  @Test(description = "Table assertions use native waiting for delayed headers and rows")
  public void waitsForTableStateWithOneRootCondition() {
    Selenide.executeJavaScript("window.prepareDelayedAssertionHeader()");
    table.$$(":scope > thead th").shouldHave(exactTexts(
        "Name", "Status", "Action", "Input", "Check", "Radio", "Select", "Read only", "Link"),
        Duration.ofSeconds(2));

    Selenide.executeJavaScript("window.prepareDelayedAssertionRow()");
    rows().shouldHave(size(2), Duration.ofSeconds(2));
    table.$$(byRole("columnheader")).shouldHave(itemWithText("Status"));
    rows().findBy(text("Beta")).should(exist);
  }

  @Test(description = "Row and cell assertions wait natively for delayed cell state")
  public void waitsForRowAndCellState() {
    Selenide.executeJavaScript("window.prepareDelayedAssertionCell()");

    cell(0, "Status").shouldHave(exactText("Ready"), Duration.ofSeconds(2));
    cells(rows().get(1)).shouldHave(exactTexts(
        "Beta", "Ready", "", "", "", "", "", "fixed too", ""));
    cell(0, "Status").shouldHave(exactText("Ready"));
  }

  @Test(description = "Row, cell, and ordered duplicate column handles survive table remount")
  public void reResolvesEveryHandleAfterRemount() {
    SelenideElement row = rows().get(0);
    SelenideElement cell = cell(0, "Status");
    ElementsCollection column = table.$$(":scope > tbody > tr > td:nth-child("
        + (columnIndex("Status") + 1) + ")");

    Selenide.executeJavaScript("window.remountAssertionTable()");

    cells(row).get(columnIndex("Name")).shouldHave(exactText("Alpha"));
    cell.shouldBe(visible).shouldHave(exactText("Ready"));
    column.shouldHave(exactTexts("Ready", "Ready"));
  }

  @Test(description = "Button and link capabilities resolve and act on embedded controls")
  public void actsOnButtonsAndLinks() {
    cell(0, "Action").$("button").shouldBe(enabled).click();
    $("#table-action-result").shouldHave(exactText("button"));

    cell(0, "Link").$("a").shouldHave(text("Details")).click();
    $("#table-action-result").shouldHave(exactText("link"));
  }

  @Test(description = "Input, checkbox, radio, and select use distinct typed capabilities")
  public void editsExplicitEmbeddedControls() {
    cell(0, "Input").$("input").setValue("changed");
    cell(0, "Input").$("input").shouldHave(value("changed"));

    cell(0, "Check").$("input[type=checkbox]").setSelected(true);
    cell(0, "Check").$("input[type=checkbox]").shouldBe(checked);

    cell(0, "Radio").$("input[type=radio]").click();
    cell(0, "Radio").$("input[type=radio]").shouldBe(selected);

    cell(0, "Select").$("select").selectOption("Two");
    Assertions.assertThat(cell(0, "Select").$("select").getSelectedOptionText()).isEqualTo("Two");
  }

  @Test(description = "Contenteditable uses inherited HTML semantics and text editing")
  public void editsContenteditableCells() {
    SelenideElement contenteditable = $("#contenteditable-table");
    ElementsCollection cells = cells(contenteditable.$$(":scope > tbody > tr").get(0));
    ElementsCollection headers = contenteditable.$$(":scope > thead th");
    headers.shouldHave(exactTexts("Direct", "Inherited", "Empty", "Plaintext", "False"));

    cells.get(headers.texts().indexOf("Direct")).shouldHave(exactText("Direct"));
    cells.get(headers.texts().indexOf("Inherited")).shouldHave(exactText("Inherited"));
    cells.get(headers.texts().indexOf("Empty")).shouldHave(exactText("Empty"));
    cells.get(headers.texts().indexOf("Plaintext")).shouldHave(exactText("Plaintext"));
    cells.get(headers.texts().indexOf("Direct")).setValue("Changed");
    cells.get(headers.texts().indexOf("Direct")).shouldHave(exactText("Changed"));
    cells.get(headers.texts().indexOf("False")).shouldHave(attribute("contenteditable", "false"));
  }

  @Test(description = "Read-only cells reject the explicit edit capability")
  public void rejectsReadOnlyEditingExplicitly() {
    SelenideElement readOnly = cell(0, "Read only");

    readOnly.shouldHave(exactText("fixed"));
    readOnly.$$("input, select, textarea, [contenteditable]").shouldHave(size(0));
  }

  @Test(description = "Assertion errors identify table, row, typed column, and actual values")
  public void reportsDiagnosticAddressesAndValues() {
    Assertions.assertThatThrownBy(() -> cells(rows().get(0))
            .shouldHave(exactTexts("wrong"), Duration.ofMillis(50)))
        .hasMessageContaining("#assertion-actions")
        .hasMessageContaining("Alpha")
        .hasMessageContaining("Ready");

    Assertions.assertThatThrownBy(() -> table.$$(":scope > tbody > tr > td:nth-child("
                + (columnIndex("Status") + 1) + ")")
            .shouldHave(exactTexts("wrong"), Duration.ofMillis(50)))
        .hasMessageContaining("#assertion-actions")
        .hasMessageContaining("Ready");
  }

  @Test(description = "Condition cell text stays on its captured row snapshot")
  public void keepsIndexedAndTypedConditionReadsOnCapturedSnapshot() {
    openQueriesFixture();
    SelenideElement query = $("#query-classic");
    ElementsCollection headers = query.$$(byRole("columnheader")).shouldHave(itemWithText("Country"));
    SelenideElement firstRow = query.$$(":scope > tbody > tr").get(0);
    WebElement indexed = cells(firstRow).get(0).toWebElement();
    WebElement typed = cells(firstRow).get(headers.texts().indexOf("Country")).toWebElement();

    Selenide.executeJavaScript("window.redirectQueryClassicRoot()");

    Assertions.assertThat(indexed.getText()).isEqualTo("Austria");
    Assertions.assertThat(typed.getText()).isEqualTo("Austria");
    cells($("#query-classic").$$(":scope > tbody > tr").get(0)).get(0).shouldHave(exactText("Changed"));
  }

  private ElementsCollection rows() {
    return table.$$(":scope > tbody > tr");
  }

  private static ElementsCollection cells(SelenideElement row) {
    return row.$$(":scope > td");
  }

  private SelenideElement cell(int row, String header) {
    return cells(rows().get(row)).get(columnIndex(header));
  }

  private int columnIndex(String header) {
    ElementsCollection headers = table.$$(byRole("columnheader")).shouldHave(itemWithText(header));
    return headers.texts().indexOf(header);
  }
}
