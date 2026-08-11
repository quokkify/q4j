package dev.quokkify.model;

import java.time.Duration;

/**
 * Single source of truth for the default timeout/polling-interval pair used by table row
 * waits ({@code BaseTable}) and step verifications ({@code Verification}), so the two
 * literals can't silently drift apart.
 */
public final class WaitDefaults {

  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
  public static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofMillis(500);

  private WaitDefaults() {
  }
}
