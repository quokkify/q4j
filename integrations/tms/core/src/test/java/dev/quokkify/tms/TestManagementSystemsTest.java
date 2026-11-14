package dev.quokkify.tms;

import java.util.List;

import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestManagementSystemsTest {

  @TmsLink("TMS_RESOLVE_1")
  @Test(description = "The only configured provider on the classpath is used")
  public void singleConfiguredProviderIsUsed() {
    TestManagementSystem provider = new FirstProvider(true);

    assertThat(TestManagementSystems.resolve(List.of(provider), false))
        .as("Resolved provider")
        .containsSame(provider);
  }

  @TmsLink("TMS_RESOLVE_2")
  @Test(description = "Two providers on the classpath fail with both provider class names")
  public void twoProvidersFail() {
    List<TestManagementSystem> providers = List.of(new FirstProvider(true), new SecondProvider());

    assertThatThrownBy(() -> TestManagementSystems.resolve(providers, false))
        .isInstanceOf(TmsConfigurationException.class)
        .hasMessageContaining("exactly one TMS provider")
        .hasMessageContaining(FirstProvider.class.getName())
        .hasMessageContaining(SecondProvider.class.getName());
  }

  @TmsLink("TMS_RESOLVE_3")
  @Test(description = "Without a provider nothing is resolved")
  public void noProviderIsEmpty() {
    assertThat(TestManagementSystems.resolve(List.of(), false)).isEmpty();
  }

  @TmsLink("TMS_RESOLVE_4")
  @Test(description = "Without a provider resolution fails when TMS_REQUIRED is true")
  public void noProviderFailsWhenRequired() {
    assertThatThrownBy(() -> TestManagementSystems.resolve(List.of(), true))
        .isInstanceOf(TmsConfigurationException.class);
  }

  @TmsLink("TMS_RESOLVE_5")
  @Test(description = "A provider without required settings is not used")
  public void notConfiguredProviderIsEmpty() {
    assertThat(TestManagementSystems.resolve(List.of(new FirstProvider(false)), false)).isEmpty();
  }

  @TmsLink("TMS_RESOLVE_6")
  @Test(description = "A provider without required settings fails resolution when TMS_REQUIRED is true")
  public void notConfiguredProviderFailsWhenRequired() {
    List<TestManagementSystem> providers = List.of(new FirstProvider(false));

    assertThatThrownBy(() -> TestManagementSystems.resolve(providers, true))
        .isInstanceOf(TmsConfigurationException.class)
        .hasMessageContaining("first");
  }

  private record FirstProvider(boolean isConfigured) implements TestManagementSystem {

    @Override
    public String id() {
      return "first";
    }

    @Override
    public void report(TmsResult result) {
    }
  }

  private static final class SecondProvider implements TestManagementSystem {

    @Override
    public String id() {
      return "second";
    }

    @Override
    public boolean isConfigured() {
      return true;
    }

    @Override
    public void report(TmsResult result) {
    }
  }
}
