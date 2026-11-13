package dev.quokkify.testplanit.services;

/**
 * Failure of a request to TestPlanIt.
 */
public class TestPlanItException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TestPlanItException(String message) {
    super(message);
  }

  public TestPlanItException(String message, Throwable cause) {
    super(message, cause);
  }
}
