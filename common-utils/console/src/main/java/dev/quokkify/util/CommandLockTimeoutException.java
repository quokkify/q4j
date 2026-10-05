package dev.quokkify.util;

import java.time.Duration;

/**
 * Thrown when an identical command on the same target is still running after the queue timeout.
 */
public final class CommandLockTimeoutException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  CommandLockTimeoutException(String target, String command, Duration timeout) {
    super("Timed out after %d ms waiting to execute command on target '%s': %s"
        .formatted(timeout.toMillis(), target, command));
  }
}
