package dev.quokkify.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serializes identical commands sent to the same target without blocking unrelated commands.
 *
 * <p>Each key owns a lock that lives only while a caller holds or waits for it; the entry is
 * removed when its last reference is released.</p>
 */
final class CommandLockManager {

  private final ConcurrentHashMap<CommandLockKey, LockEntry> locks = new ConcurrentHashMap<>();

  LockHandle acquire(CommandLockKey key, Duration timeout) throws InterruptedException {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(timeout, "timeout");
    LockEntry entry = locks.compute(key, (ignored, current) -> {
      LockEntry selected = current == null ? new LockEntry() : current;
      selected.references++;
      return selected;
    });
    boolean acquired = false;
    try {
      acquired = entry.lock.tryLock(timeout.toNanos(), TimeUnit.NANOSECONDS);
      if (!acquired) {
        throw new CommandLockTimeoutException(key.target(), key.normalizedCommand(), timeout);
      }
      return new LockHandle(this, key, entry);
    } finally {
      if (!acquired) {
        releaseReference(key, entry);
      }
    }
  }

  int trackedKeys() {
    return locks.size();
  }

  private void release(CommandLockKey key, LockEntry entry) {
    entry.lock.unlock();
    releaseReference(key, entry);
  }

  private void releaseReference(CommandLockKey key, LockEntry entry) {
    locks.compute(key, (ignored, current) -> {
      if (current != entry) {
        throw new IllegalStateException("Command lock entry changed while in use: " + key);
      }
      entry.references--;
      return entry.references == 0 ? null : entry;
    });
  }

  record CommandLockKey(String target, String normalizedCommand) {

    static CommandLockKey of(String target, String command) {
      Objects.requireNonNull(target, "target");
      Objects.requireNonNull(command, "command");
      String normalizedCommand = command.replace("\r\n", "\n").replace('\r', '\n').strip();
      return new CommandLockKey(target.strip(), normalizedCommand);
    }
  }

  static final class LockHandle implements AutoCloseable {

    private final CommandLockManager manager;
    private final CommandLockKey key;
    private final LockEntry entry;
    private boolean closed;

    private LockHandle(CommandLockManager manager, CommandLockKey key, LockEntry entry) {
      this.manager = manager;
      this.key = key;
      this.entry = entry;
    }

    @Override
    public synchronized void close() {
      if (!closed) {
        manager.release(key, entry);
        closed = true;
      }
    }
  }

  private static final class LockEntry {

    private final ReentrantLock lock = new ReentrantLock();
    private int references;
  }
}
