package dev.quokkify.testrail.tms;

import dev.quokkify.testrail.configs.TestRailConfiguration;
import dev.quokkify.testrail.utils.TestRailHelper;
import dev.quokkify.tms.TmsResult;
import dev.quokkify.tms.TmsStatus;

import io.qameta.allure.TmsLink;
import org.mockito.MockedStatic;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class TestRailManagementSystemTest {

  @TmsLink("TR_SYSTEM_2")
  @Test(description = "TestRail is configured with the same rule as its listeners: not disabled and TESTRAIL_ID set")
  public void configuredLikeTestRailListeners() {
    assertThat(new TestRailManagementSystem(config(false, "12")).isConfigured()).isTrue();
    assertThat(new TestRailManagementSystem(config(false, "")).isConfigured()).isFalse();
    assertThat(new TestRailManagementSystem(config(true, "12")).isConfigured()).isFalse();
  }

  private static TestRailConfiguration config(boolean disabled, String testRailId) {
    TestRailConfiguration config = mock(TestRailConfiguration.class);
    when(config.isTestrailDisabled()).thenReturn(disabled);
    when(config.testRailId()).thenReturn(testRailId);
    return config;
  }

  @TmsLink("TR_SYSTEM_3")
  @Test(description = "shouldRun delegates to the TestRail run filter")
  public void shouldRunDelegatesToRunFilter() {
    TestRailManagementSystem system = new TestRailManagementSystem(config(false, "1"), "5"::equals);

    assertThat(system.shouldRun("5")).isTrue();
    assertThat(system.shouldRun("6")).isFalse();
  }

  @TmsLink("TR_SYSTEM_4")
  @Test(description = "BLOCKED result of a case outside the TestRail run is not reported")
  public void blockedResultHonoursRunFilter() {
    TestRailManagementSystem system = new TestRailManagementSystem(config(false, "1"), "5"::equals);

    try (MockedStatic<TestRailHelper> helper = mockStatic(TestRailHelper.class)) {
      system.report(new TmsResult("6", TmsStatus.BLOCKED, "disabled", null));
      helper.verifyNoInteractions();

      system.report(new TmsResult("5", TmsStatus.BLOCKED, "disabled", null));
      helper.verify(() -> TestRailHelper.addTestResultForDisabledTest(eq("5"), anyInt(), eq("disabled")));
    }
  }
}
