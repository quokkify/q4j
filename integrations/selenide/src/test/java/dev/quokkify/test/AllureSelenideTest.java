package dev.quokkify.test;

import com.codeborne.selenide.logevents.SelenideLogger;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class AllureSelenideTest extends BaseTest {

  @Test(description = "Verify the native Allure listener is registered on the current TestNG worker thread")
  public void testAllureSelenideListenerIsRegistered() {
    Assertions.assertThat(SelenideLogger.hasListener("AllureSelenide")).isTrue();
  }
}
