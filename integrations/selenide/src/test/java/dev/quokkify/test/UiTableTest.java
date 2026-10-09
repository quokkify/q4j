package dev.quokkify.test;

import java.time.Duration;

import dev.quokkify.elements.table.Table;
import dev.quokkify.elements.table.TableLayout;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.ElementNotFound;
import io.qameta.allure.TmsLink;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
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
    Table customers = customers();

    customers.row("Company", firm.company()).cell("Contact").shouldHave(exactText(firm.contact()), TIMEOUT);
    customers.row("Company", firm.company()).cell("Country").shouldHave(exactText(firm.country()), TIMEOUT);
    customers.column("Company").shouldHave(exactTexts("Alfreds Futterkiste", "Ernst Handel"), TIMEOUT);
  }

  @TmsLink("UI_ID_4")
  @Test(description = "Verify local TABLE map lookup")
  public void testVerifyRow() {
    openPage();
    Table customers = customers();

    customers.rows("Company", "Ernst Handel").filterBy(text("Austria")).shouldHave(size(1), TIMEOUT);
    customers.row("Company", "Ernst Handel").cell("Contact").shouldHave(exactText("Roland Mendel"));
  }

  @Test(description = "Verify DYNAMIC TABLE maps displayed headers and FLEX TABLE excludes its header row")
  public void testDynamicAndFlexTables() {
    openPage();
    Table customers = customers();
    Table flex = Table.of($("#flex-customers"), TableLayout.of(
        By.cssSelector(":scope > .flex-table-row:not(:first-child)"), By.cssSelector(":scope > div"),
        By.cssSelector(":scope > .flex-table-row:first-child > div")));

    customers.row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
    flex.row("Company", "Ernst Handel").cell("Country").shouldHave(exactText("Austria"), TIMEOUT);
    flex.headers().shouldHave(exactTexts("Company", "Contact", "Country"), TIMEOUT);
    flex.rows().shouldHave(size(2), TIMEOUT);
    assertThatThrownBy(() -> flex.row("Company", "Company").self().should(exist, Duration.ofMillis(600)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Company");
  }

  @Test(description = "Verify missing local TABLE row reports a negative lookup")
  public void testMissingTableRow() {
    openPage();

    assertThatThrownBy(() -> customers().row("Company", "Missing Company").self()
        .should(exist, Duration.ofMillis(600)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Missing Company");
  }

  private static Table customers() {
    return Table.of($("#customers"), TableLayout.html());
  }

  private void openPage() {
    Selenide.open(APP_CONFIG.baseUrl() + TABLE_FIXTURE_PATH);
  }

  public record Firm(String company, String contact, String country) {
  }
}
