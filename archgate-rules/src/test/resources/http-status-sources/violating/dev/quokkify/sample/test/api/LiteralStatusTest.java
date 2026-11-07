package dev.quokkify.sample.test.api;

import org.apache.http.HttpStatus;
import org.testng.annotations.Test;

public class LiteralStatusTest {

  @Test
  public void passesLiterals() {
    steps.verify().verifyResponseStatusCode(steps.getHealth(), 200);
    steps.verify().verifyStatus(response, 401);
  }

  @Test
  public void ignoresConstantsAndUnrelatedNumbers() {
    steps.verify().verifyResponseStatusCode(response, HttpStatus.SC_OK);
    steps.listProducts(200);
    steps.verify().verifyStatus(response, 42);
  }
}
