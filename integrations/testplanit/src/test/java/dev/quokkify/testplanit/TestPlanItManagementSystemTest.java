package dev.quokkify.testplanit;

import dev.quokkify.testplanit.configs.TestPlanItConfiguration;

import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestPlanItManagementSystemTest {

  @TmsLink("TPI_SYSTEM_1")
  @Test(description = "The system id is testplanit")
  public void idIsTestplanit() {
    assertThat(new TestPlanItManagementSystem(configured()).id()).isEqualTo("testplanit");
  }

  @TmsLink("TPI_SYSTEM_2")
  @Test(description = "The system is configured only with URL, token and project, and when not disabled")
  public void requiresUrlTokenAndProject() {
    assertThat(new TestPlanItManagementSystem(configured()).isConfigured()).isTrue();

    TestPlanItConfiguration withoutToken = configured();
    when(withoutToken.apiToken()).thenReturn("");
    assertThat(new TestPlanItManagementSystem(withoutToken).isConfigured()).isFalse();

    TestPlanItConfiguration withoutProject = configured();
    when(withoutProject.projectId()).thenReturn(null);
    assertThat(new TestPlanItManagementSystem(withoutProject).isConfigured()).isFalse();

    TestPlanItConfiguration disabled = configured();
    when(disabled.isTestPlanItDisabled()).thenReturn(true);
    assertThat(new TestPlanItManagementSystem(disabled).isConfigured()).isFalse();
  }

  private static TestPlanItConfiguration configured() {
    TestPlanItConfiguration config = mock(TestPlanItConfiguration.class);
    when(config.isTestPlanItDisabled()).thenReturn(false);
    when(config.apiUrl()).thenReturn("http://localhost:30000");
    when(config.apiToken()).thenReturn("tpi_test");
    when(config.projectId()).thenReturn(1);
    return config;
  }
}
