package dev.quokkify.test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import dev.quokkify.elements.table.HorizontalTable;

import com.codeborne.selenide.Selenide;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Selenide.$;

public class UiHorizontalTableTest extends BaseTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  @TmsLink("UI_ID_5")
  @Test(description = "Verify local HORIZONTAL TABLE and DYNAMIC HORIZONTAL TABLE rows")
  public void testTable() {
    openPage();
    HorizontalTable customers = customers();

    customers.value("Name").shouldHave(exactText("Bill Gates"), TIMEOUT);
    customers.value("Telephone 1").shouldHave(exactText("555 77 854"), TIMEOUT);
    customers.value("Telephone 2").shouldHave(exactText("555 77 855"), TIMEOUT);
    customers.headers().shouldHave(size(3), TIMEOUT);
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("Name", "Bill Gates");
    expected.put("Telephone 1", "555 77 854");
    expected.put("Telephone 2", "555 77 855");
    Map<String, String> actual = new LinkedHashMap<>();
    customers.headers().texts().forEach(header -> actual.put(header, customers.value(header).text()));
    Assertions.assertThat(actual)
        .containsExactlyEntriesOf(expected);
  }

  @Test(description = "Verify missing local HORIZONTAL TABLE row is reported")
  public void testMissingRow() {
    openPage();

    Assertions.assertThat(customers().value("Missing Header").exists())
        .isFalse();
  }

  private static HorizontalTable customers() {
    return HorizontalTable.of($("#horizontal-customers"));
  }

  private void openPage() {
    Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html");
  }
}
