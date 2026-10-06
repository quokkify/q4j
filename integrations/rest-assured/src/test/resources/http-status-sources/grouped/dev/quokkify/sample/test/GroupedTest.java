package dev.quokkify.sample.test;

import org.testng.annotations.Test;

public class GroupedTest {

  @Test(groups = {"smoke", "api"})
  public void apiTest() {
    steps.verify().verifyResponseStatusCode(response, 404);
  }

  @Test
  public void unitTest() {
    assertStatus(500);
  }
}
