package dev.quokkify.test;

import java.time.Duration;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.testng.annotations.Test;

public class AllureFailureDemoTest extends BaseTest {

  private static final String TABLE_FIXTURE_PATH = "/table/delayed-table.html";

  @Test(description = "INTENTIONAL FAILURE DEMO: show Selenide screenshot and page-source attachments")
  public void demonstrateAllureSelenideFailureAttachments() {
    Selenide.open(APP_CONFIG.baseUrl() + TABLE_FIXTURE_PATH);

    Selenide.$("#customers")
        .shouldHave(Condition.text("INTENTIONAL FAILURE: this marker must not exist"), Duration.ofSeconds(1));
  }
}
