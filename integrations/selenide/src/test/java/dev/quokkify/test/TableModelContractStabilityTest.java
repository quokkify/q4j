package dev.quokkify.test;

import org.testng.annotations.Test;

/** Repeats only the two timing-sensitive contract scenarios in one browser job. */
public class TableModelContractStabilityTest extends TableModelContractTest {

  @Test(invocationCount = 50, threadPoolSize = 1,
      description = "Stress the delayed-row protocol without retrying failures")
  public void waitsForDelayedRow() {
    super.waitsForDelayedRow();
  }

  @Test(invocationCount = 50, threadPoolSize = 1,
      description = "Stress the remounted-row reference protocol without retrying failures")
  public void rowReferenceSurvivesRemount() {
    super.rowReferenceSurvivesRemount();
  }
}
