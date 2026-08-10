package dev.quokkify.test;

import java.util.Objects;

import dev.quokkify.config.BrowserConfiguration;
import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.config.Configuration;
import dev.quokkify.service.Browser;
import dev.quokkify.service.steps.google.GoogleNavigationSteps;
import dev.quokkify.service.steps.w3schools.W3SchoolsNavigationSteps;

import com.codeborne.selenide.Selenide;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;

public class BaseTest {

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

  @AfterMethod(alwaysRun = true)
  protected void closeWebDriver() {
    Selenide.closeWebDriver();
  }
}
