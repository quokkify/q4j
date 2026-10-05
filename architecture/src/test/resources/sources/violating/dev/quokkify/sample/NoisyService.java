package dev.quokkify.sample;

import java.util.List;

public class NoisyService {

  public void run() {
    System.out.println("started");
    try {
      work();
    } catch (IllegalStateException failure) {
      java.lang.System.err.println("failed");
      failure.printStackTrace();
      failure.printStackTrace(System.err);
    }
    List.of("a").forEach(System.out::println);
    Thread.dumpStack();
  }

  private void work() {
  }
}
