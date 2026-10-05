package dev.quokkify.architecture.fixtures.taikai.clean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Fixture for {@code TaikaiArchitectureRuleTest}: a logger that follows the convention.
 */
public class QuietLogging {

  private static final Logger LOG = LogManager.getLogger(QuietLogging.class);

  /**
   * Logs one line.
   */
  public void run() {
    LOG.info("run");
  }
}
