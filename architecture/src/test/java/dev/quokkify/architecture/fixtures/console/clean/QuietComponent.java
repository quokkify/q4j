package dev.quokkify.architecture.fixtures.console.clean;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Fixture for {@code NoConsoleOutputRuleTest}: a stack trace rendered into a target the caller chose.
 */
public class QuietComponent {

  /**
   * Renders the stack trace of a failure into a string.
   *
   * @param failure failure to describe
   * @return the rendered stack trace
   */
  public String describe(Exception failure) {
    StringWriter trace = new StringWriter();
    failure.printStackTrace(new PrintWriter(trace));
    return trace.toString();
  }
}
