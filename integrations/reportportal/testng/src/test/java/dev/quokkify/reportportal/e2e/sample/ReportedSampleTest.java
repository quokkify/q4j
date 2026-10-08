package dev.quokkify.reportportal.e2e.sample;

import io.qameta.allure.TmsLink;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Executed only in a child JVM by {@code ReportPortalAgentEndToEndTest}; excluded from the module test task.
 */
public class ReportedSampleTest {

  @TmsLink("RP_E2E_SAMPLE")
  @Test(description = "Sample passing test")
  public void passingTest() {
  }

  @Test(description = "Sample failing test")
  public void failingTest() {
    throw new AssertionError("expected sample failure");
  }

  @Test(description = "Sample skipped test")
  public void skippedTest() {
    throw new SkipException("expected sample skip");
  }
}
