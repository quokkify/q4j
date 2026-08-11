package dev.quokkify.provider;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Thread-safe base class for lockable resources.
 *
 * <p>Provides non-blocking lock semantics via {@link AtomicBoolean} and
 * tracks last update timestamp. Use {@link #tryLock()} and {@link #unlock()}
 * to coordinate access across threads.</p>
 */
public abstract class LockingResource {

  /**
   * Indicates whether the resource is currently free to be acquired.
   * Exposed to JSON as a boolean field if needed by clients.
   */
  private final AtomicBoolean free = new AtomicBoolean(true);

  /**
   * Last update timestamp (stored using the project's default timezone adapter).
   */
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private volatile LocalDateTime updatedAt = LocalDateTime.now(ZoneId.systemDefault());

  /** Attempts to acquire the resource. Returns true on success, false otherwise. */
  public boolean tryLock() {
    boolean acquired = free.compareAndSet(true, false);
    if (acquired) {
      touch();
    }
    return acquired;
  }

  /** Releases the resource if held. Idempotent (safe to call multiple times). */
  public void unlock() {
    free.set(true);
    touch();
  }

  /** Updates the last-modified timestamp to now. */
  public void touch() {
    updatedAt = LocalDateTime.now(ZoneId.systemDefault());
  }

  /** Returns whether the resource is currently free. */
  public boolean isFree() {
    return free.get();
  }

  /** Returns the last update timestamp. */
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  /** Allows tests or custom flows to set the timestamp explicitly (optional). */
  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
