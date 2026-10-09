package dev.quokkify.test;

import java.time.Duration;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UiTableTest extends BaseTest {

  private static final String TABLE_FIXTURE_PATH = "/table/delayed-table.html";
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  @TmsLink("UI_ID_3")
  @Test(description = "Verify local TABLE row lookup, cells, and column values")
  public void testTable() {
    openPage();
    Firm firm = new Firm("Ernst Handel", "Roland Mendel", "Austria");
    SelenideElement table = $("#customers");

    table.$$("tbody tr").findBy(text(firm.company()))
        .$$("td").get(columnIndex(table, "Contact"))
        .shouldHave(exactText(firm.contact()), TIMEOUT);
    table.$$("tbody tr").findBy(text(firm.company()))
        .$$("td").get(columnIndex(table, "Country"))
        .shouldHave(exactText(firm.country()), TIMEOUT);
    table.$$("tbody tr > td:nth-child(" + (columnIndex(table, "Company") + 1) + ")")
        .shouldHave(exactTexts("Alfreds Futterkiste", "Ernst Handel"), TIMEOUT);
  }

  @TmsLink("UI_ID_4")
  @Test(description = "Verify local TABLE map lookup")
  public void testVerifyRow() {
    openPage();
    SelenideElement table = $("#customers");

    ElementsCollection rows = table.$$("tbody tr")
        .filterBy(text("Ernst Handel"))
        .filterBy(text("Austria"));

    rows.shouldHave(size(1), TIMEOUT);
    rows.first().$$("td").get(columnIndex(table, "Contact")).shouldHave(exactText("Roland Mendel"));
  }

  @Test(description = "Verify DYNAMIC TABLE maps displayed headers and FLEX TABLE excludes its header row")
  public void testDynamicAndFlexTables() {
    openPage();
    SelenideElement table = $("#customers");
    SelenideElement flexTable = $("#flex-customers");
    ElementsCollection flexDataRows = flexTable.$$(":scope > .flex-table-row:not(:first-child)");

    table.$$("tbody tr").findBy(text("Ernst Handel"))
        .$$("td").get(columnIndex(table, "Country"))
        .shouldHave(exactText("Austria"), TIMEOUT);
    flexDataRows.findBy(text("Ernst Handel"))
        .$$(":scope > div").get(2)
        .shouldHave(exactText("Austria"), TIMEOUT);
    flexTable.$$(":scope > .flex-table-row > div:nth-child(1)")
        .excludeWith(exactText("Company"))
        .shouldHave(exactTexts("Alfreds Futterkiste", "Ernst Handel"), TIMEOUT);
    assertThatThrownBy(() -> flexDataRows.findBy(text("Company")).should(exist, Duration.ofMillis(600)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Company");
  }

  @Test(description = "Verify missing local TABLE row reports a negative lookup")
  public void testMissingTableRow() {
    openPage();

    assertThatThrownBy(() -> $("#customers").$$("tbody tr").findBy(text("Missing Company"))
        .should(exist, Duration.ofMillis(600)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Missing Company");
  }

  private static int columnIndex(SelenideElement table, String header) {
    return table.$$("th").shouldHave(itemWithText(header)).texts().indexOf(header);
  }

  private void openPage() {
    Selenide.open(APP_CONFIG.baseUrl() + TABLE_FIXTURE_PATH);
  }

  public record Firm(String company, String contact, String country) {
  }
}
