package dev.quokkify.sample.test.api;

import org.apache.http.HttpStatus;
import org.testng.annotations.Test;

public class ConstantStatusTest {

  @Test
  public void passesConstants() {
    steps.verify().verifyResponseStatusCode(response, HttpStatus.SC_UNAUTHORIZED);
  }
}
