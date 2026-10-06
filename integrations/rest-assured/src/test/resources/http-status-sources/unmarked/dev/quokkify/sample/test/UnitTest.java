package dev.quokkify.sample.test;

import org.testng.annotations.Test;

public class UnitTest {

  @Test
  public void mapsStatus() {
    assertStatus(500);
  }
}
