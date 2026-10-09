package dev.quokkify.test;

import java.time.Duration;

import dev.quokkify.elements.table.HorizontalTable;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.ElementNotFound;
import org.testng.annotations.Test;

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
    Selenide.executeJavaScript("window.reloadHorizontalTable()");
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

  private static HorizontalTable openHorizontal() {
    Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html");
    return HorizontalTable.of($("#horizontal-customers"));
  }
}
