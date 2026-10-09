package dev.quokkify.test;

import java.time.Duration;

import dev.quokkify.elements.table.HorizontalTable;

import com.codeborne.selenide.Selenide;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Selenide.$;

public class ReproHorizontalAsyncTest extends BaseTest {

  @Test(description = "FIXED: requiredRow waits for a delayed HORIZONTAL column row (N1)")
  public void requiredRowWaitsForDelayedHorizontalRow() {
    Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html");

    HorizontalTable customers = HorizontalTable.of($("#horizontal-customers"));

    customers.value("Telephone 2").shouldHave(exactText("555 77 855"), Duration.ofSeconds(5));

    Assertions.assertThat(customers.value("Telephone 2").text()).isEqualTo("555 77 855");
  }
}
