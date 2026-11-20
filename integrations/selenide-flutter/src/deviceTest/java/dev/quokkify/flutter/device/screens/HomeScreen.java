package dev.quokkify.flutter.device.screens;

import java.time.Duration;

import dev.quokkify.flutter.device.ModeLocators;
import dev.quokkify.impl.Page;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.appium.SelenideAppium;
import io.qameta.allure.Step;

public class HomeScreen implements Page {

  private static final Duration START_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration BACK_TIMEOUT = Duration.ofSeconds(2);
  private static final int MAX_BACKS = 4;

  private final ModeLocators locators;

  public HomeScreen(ModeLocators locators) {
    this.locators = locators;
  }

  /**
   * Launches the app and waits for the home screen when no driver runs, otherwise presses back until it shows.
   */
  @Step("Go to the home screen")
  public static HomeScreen reach(ModeLocators locators) {
    HomeScreen home = new HomeScreen(locators);
    if (!WebDriverRunner.hasWebDriverStarted()) {
      SelenideAppium.launchApp();
      home.entry("home_login").shouldBe(Condition.visible, START_TIMEOUT);
      return home;
    }
    for (int i = 0; i < MAX_BACKS && !home.entry("home_login").is(Condition.visible, BACK_TIMEOUT); i++) {
      Selenide.back();
    }
    home.entry("home_login").shouldBe(Condition.visible);
    return home;
  }

  public LoginScreen openLogin() {
    open("home_login");
    return new LoginScreen(locators);
  }

  public DelayedScreen openDelayed() {
    open("home_delayed");
    return new DelayedScreen(locators);
  }

  public LongListScreen openLongList() {
    open("home_list");
    return new LongListScreen(locators);
  }

  public DuplicatesScreen openDuplicates() {
    open("home_duplicates");
    return new DuplicatesScreen(locators);
  }

  public LabelOnlyScreen openLabelOnly() {
    open("home_label");
    return new LabelOnlyScreen(locators);
  }

  public OffstageScreen openOffstage() {
    open("home_offstage");
    return new OffstageScreen(locators);
  }

  private void open(String id) {
    entry(id).click();
  }

  private SelenideElement entry(String id) {
    return SelenideAppium.$(locators.id(id));
  }
}
