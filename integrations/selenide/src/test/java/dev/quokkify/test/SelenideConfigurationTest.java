package dev.quokkify.test;

import com.codeborne.selenide.Configuration;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class SelenideConfigurationTest extends BaseTest {

  @Test
  public void usesEagerPageLoadStrategyBeforeOpeningBrowser() {
    Assertions.assertThat(Configuration.pageLoadStrategy).isEqualTo("eager");
  }
}
