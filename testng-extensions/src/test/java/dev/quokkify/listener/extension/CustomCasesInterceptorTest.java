package dev.quokkify.listener.extension;

import java.util.List;

import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class CustomCasesInterceptorTest {

  @TmsLink("TNG_TMS_LINKS_5")
  @Test(description = "Test is required when its second link is requested")
  public void interceptorKeepsSecondLink() {
    Assertions.assertThat(CustomCasesInterceptor.isTestRequired(List.of("6", "7"), List.of("7"))).isTrue();
  }
}
