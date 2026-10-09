package dev.quokkify.test;

import java.time.Duration;

import com.codeborne.selenide.Selenide;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class ReproHorizontalAsyncTest extends BaseTest {

  @Test(description = "FIXED: requiredRow waits for a delayed HORIZONTAL column row (N1)")
  public void requiredRowWaitsForDelayedHorizontalRow() {
    Selenide.open(APP_CONFIG.baseUrl() + "/table/delayed-table.html");

    $("#horizontal-customers").$$("tr").findBy(text("Telephone 2")).$("td")
        .shouldHave(exactText("555 77 855"), Duration.ofSeconds(5));

    Assertions.assertThat($("#horizontal-customers").$$("tr").findBy(text("Telephone 2")).$("td").text())
        .isEqualTo("555 77 855");
  }
}
