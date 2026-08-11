package dev.quokkify.test;

import java.util.Objects;

import dev.quokkify.config.BrowserConfiguration;
import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.config.Configuration;
import dev.quokkify.service.Browser;
import dev.quokkify.service.steps.google.GoogleNavigationSteps;
import dev.quokkify.service.steps.w3schools.W3SchoolsNavigationSteps;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.selenide.AllureSelenide;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

public class BaseTest {

  private static final String ALLURE_LISTENER = "AllureSelenide";

  protected static final Configuration APP_CONFIG = ConfigRegistry.get(Configuration.class);
  protected static final BrowserConfiguration BROWSER_CONFIGURATION = ConfigRegistry.get(BrowserConfiguration.class);

  protected GoogleNavigationSteps googleNavigationSteps = new GoogleNavigationSteps(APP_CONFIG.baseUrl());
  protected W3SchoolsNavigationSteps w3SchoolsNavigationSteps = new W3SchoolsNavigationSteps(APP_CONFIG.baseUrl());

  @BeforeSuite(alwaysRun = true)
  protected void beforeSuite() {
    Browser.setDefaultConfigurations();
    com.codeborne.selenide.Configuration.headless = true;
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
