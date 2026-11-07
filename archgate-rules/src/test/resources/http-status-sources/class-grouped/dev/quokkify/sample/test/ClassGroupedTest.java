package dev.quokkify.sample.test;

import org.testng.annotations.Test;

@Test(groups = "api")
public class ClassGroupedTest {

  public void waitsForService() {
    steps.setStatusPollIntervalMillis(300);
    steps.verify().expectStatus(response, 503);
  }
}
