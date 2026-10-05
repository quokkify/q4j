package dev.quokkify.architecture.fixtures.taikai.violating;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Fixture for {@code TaikaiArchitectureRuleTest}: a logger field not named {@code LOG}.
 */
public class LowercaseLogger {

  private static final Logger log = LogManager.getLogger(LowercaseLogger.class);

  /**
   * Logs one line.
   */
  public void run() {
    log.info("run");
  }
}
