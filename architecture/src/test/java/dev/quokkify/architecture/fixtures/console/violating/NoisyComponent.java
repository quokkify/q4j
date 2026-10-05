package dev.quokkify.architecture.fixtures.console.violating;

import java.util.List;

import static java.lang.System.out;

/**
 * Fixture for {@code NoConsoleOutputRuleTest}: every way main code can reach the console.
 */
public class NoisyComponent {

  /**
   * Writes to the console in every form the rule rejects.
   */
  public void run() {
    System.out.println("started");
    out.println("statically imported");
    List.of("a").forEach(System.err::println);
    try {
      work();
    } catch (IllegalStateException failure) {
      failure.printStackTrace();
    }
    Thread.dumpStack();
  }

  private void work() {
    throw new IllegalStateException("fixture");
  }
}
