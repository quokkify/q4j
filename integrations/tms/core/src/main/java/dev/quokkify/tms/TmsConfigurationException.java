package dev.quokkify.tms;

/**
 * Thrown when the test management system setup is invalid, for example when more than one provider is on the
 * classpath. It aborts the test run before any test is executed.
 */
public final class TmsConfigurationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TmsConfigurationException(String message) {
    super(message);
  }
}
