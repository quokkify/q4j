package dev.quokkify.test;

import java.time.Duration;

import dev.quokkify.elements.table.HorizontalTable;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.ElementNotFound;
import io.qameta.allure.Allure;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.not;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HorizontalTableTest extends BaseTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  @Test
  public void readsValueByHeader() {
    HorizontalTable h = openHorizontal();

    h.value("Name").shouldHave(exactText("Bill Gates"));
  }

  @Test
  public void waitsForDelayedHeader() {
    HorizontalTable h = openHorizontal();

    h.value("Telephone 2").shouldHave(exactText("555 77 855"), TIMEOUT);
  }

  @Test
  public void survivesRemount() {
    HorizontalTable h = openHorizontal();

    h.value("Telephone 2").shouldHave(exactText("555 77 855"), TIMEOUT);
    Allure.step("Remount horizontal table with Telephone 2 → 555 77 856",
        () -> Selenide.executeJavaScript("window.reloadHorizontalTable()"));
    h.value("Telephone 2").shouldHave(exactText("555 77 856"), TIMEOUT);
  }

  @Test
  public void missingHeaderFails() {
    HorizontalTable h = openHorizontal();

    assertThatThrownBy(() -> h.value("Missing Header").should(exist, Duration.ofMillis(600)))
        .isInstanceOf(ElementNotFound.class)
        .hasMessageContaining("Missing Header");
  }

  @Test
  public void doesNotMatchHeaderPrefix() {
    HorizontalTable h = openHorizontal();

    h.value("Telephone").should(not(exist));
  }

  @Test
  public void ignoresNestedTableLabels() {
    Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html");
    Allure.step("Inject horizontal table whose Name value holds a nested Phone table",
        () -> Selenide.executeJavaScript("document.body.innerHTML = arguments[0]", "<table id='h'>"
            + "<tr><th>Name</th><td>Bill<table><tr><th>Phone</th><td>nested</td></tr></table></td></tr>"
            + "<tr><th>Phone</th><td>555</td></tr></table>"));
    HorizontalTable h = HorizontalTable.of($("#h"));

    h.value("Phone").shouldHave(exactText("555"), TIMEOUT);
    h.headers().shouldHave(exactTexts("Name", "Phone"));
  }

  private static HorizontalTable openHorizontal() {
    Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html");
    return HorizontalTable.of($("#horizontal-customers"));
  }
}
