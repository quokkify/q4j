package dev.quokkify.test;

import java.util.Objects;

import dev.quokkify.config.BrowserConfiguration;
import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.config.Configuration;
import dev.quokkify.service.Browser;
import dev.quokkify.service.steps.google.GoogleNavigationSteps;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.selenide.AllureSelenide;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

public class BaseTest {

  private static final String ALLURE_LISTENER = "AllureSelenide";

  private static final String TABLE_MODEL_CONTRACT_ROOT = "/table-model-contract/";

  protected static final Configuration APP_CONFIG = ConfigRegistry.get(Configuration.class);
  protected static final BrowserConfiguration BROWSER_CONFIGURATION = ConfigRegistry.get(BrowserConfiguration.class);

  protected GoogleNavigationSteps googleNavigationSteps = new GoogleNavigationSteps(APP_CONFIG.baseUrl());

  protected static String nginxBaseUrl() {
    return System.getenv().getOrDefault("NGINX_BASE_URL", "http://localhost");
  }

  protected static void openTableModelContract(String page) {
    Selenide.open(nginxBaseUrl() + TABLE_MODEL_CONTRACT_ROOT + page);
  }

  protected static void openClassicVariantsFixture() {
    openTableModelContract("classic.html");
  }

  protected static void openCustomGridsFixture() {
    openTableModelContract("custom-grids.html");
  }

  protected static void openEdgeCasesFixture() {
    openTableModelContract("edge-cases.html");
  }

  protected static void openQueriesFixture() {
    openTableModelContract("queries.html");
  }

  protected static void openAssertionsFixture() {
    openTableModelContract("assertions.html");
  }

  @BeforeSuite(alwaysRun = true)
  protected void beforeSuite() {
    Browser.setDefaultConfigurations();
    com.codeborne.selenide.Configuration.headless = true;
    // Selenide 7.17.0 already contributes --disable-dev-shm-usage for Chrome. Keep only the
    // empirically required extra flag here for this headless environment.
    com.codeborne.selenide.Configuration.browserCapabilities = Browser.mergeCapabilities(new ChromeOptions()
        .addArguments("--no-sandbox"));
    if (Objects.nonNull(BROWSER_CONFIGURATION.remoteUrl())) {
      Browser.setRemoteDefaultConfiguration();
    }
  }

  @BeforeMethod(alwaysRun = true)
  protected void addAllureSelenideListener() {
    SelenideLogger.addListener(ALLURE_LISTENER, new AllureSelenide().screenshots(true).savePageSource(true));
  }

  @AfterMethod(alwaysRun = true)
  protected void closeWebDriver() {
    try {
      Selenide.closeWebDriver();
    } finally {
      SelenideLogger.removeListener(ALLURE_LISTENER);
    }
  }
}