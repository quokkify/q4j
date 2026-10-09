package dev.quokkify.test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.codeborne.selenide.Selenide;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$$;

public class UiHorizontalTableTest extends BaseTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  @TmsLink("UI_ID_5")
  @Test(description = "Verify local HORIZONTAL TABLE and DYNAMIC HORIZONTAL TABLE rows")
  public void testTable() {
    openPage();

    $$("#horizontal-customers tr").findBy(text("Name")).$("td")
        .shouldHave(exactText("Bill Gates"), TIMEOUT);
    $$("#horizontal-customers tr").findBy(text("Telephone 1")).$("td")
        .shouldHave(exactText("555 77 854"), TIMEOUT);
    $$("#horizontal-customers tr").findBy(text("Telephone 2")).$("td")
        .shouldHave(exactText("555 77 855"), TIMEOUT);
    $$("#horizontal-customers tr").shouldHave(size(3), TIMEOUT);
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("Name", "Bill Gates");
    expected.put("Telephone 1", "555 77 854");
    expected.put("Telephone 2", "555 77 855");
    Map<String, String> actual = new LinkedHashMap<>();
    $$("#horizontal-customers tr").asFixedIterable()
        .forEach(row -> actual.put(row.$("th").text(), row.$("td").text()));
    Assertions.assertThat(actual)
        .containsExactlyEntriesOf(expected);
  }

  @Test(description = "Verify missing local HORIZONTAL TABLE row is reported")
  public void testMissingRow() {
    openPage();

    Assertions.assertThat($$("#horizontal-customers tr").findBy(text("Missing Header")).exists())
        .isFalse();
  }

  private void openPage() {
    Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html");
  }
}
