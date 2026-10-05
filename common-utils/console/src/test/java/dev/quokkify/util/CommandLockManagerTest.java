package dev.quokkify.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import dev.quokkify.util.CommandLockManager.CommandLockKey;
import dev.quokkify.util.CommandLockManager.LockHandle;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class CommandLockManagerTest {

  private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(5);
  private static final long AWAIT_SECONDS = 5;

  @Test
  public void keyNormalizesTargetWhitespaceAndLineEndings() {
    CommandLockKey key = CommandLockKey.of(" host-a ", " echo ok\r\n ");

    Assertions.assertThat(key).isEqualTo(CommandLockKey.of("host-a", "echo ok"));
    Assertions.assertThat(key.target()).isEqualTo("host-a");
    Assertions.assertThat(key.normalizedCommand()).isEqualTo("echo ok");
    Assertions.assertThat(CommandLockKey.of("host-a", "a\r\nb\rc").normalizedCommand()).isEqualTo("a\nb\nc");
  }

  @Test
  public void sameTargetAndCommandAreSerialized() throws Exception {
    CommandLockManager manager = new CommandLockManager();
    CommandLockKey key = CommandLockKey.of("host-a", "echo ok");
    CountDownLatch secondEntered = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<?> second;
      Thread waiter;
      try (LockHandle ignored = manager.acquire(key, LOCK_TIMEOUT)) {
        AtomicReference<Thread> waiterThread = new AtomicReference<>();
        second = executor.submit(() -> {
          waiterThread.set(Thread.currentThread());
          runUnderLock(manager, key, secondEntered, new CountDownLatch(0));
        });
        waiter = awaitThread(waiterThread);
        awaitTimedWaiting(waiter);
        Assertions.assertThat(secondEntered.getCount()).isEqualTo(1);
      }
      Assertions.assertThat(secondEntered.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
      second.get(AWAIT_SECONDS, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void differentCommandsOnSameTargetRunInParallel() throws Exception {
    assertCanEnterTogether(CommandLockKey.of("host-a", "command A"), CommandLockKey.of("host-a", "command B"));
  }

  @Test
  public void sameCommandOnDifferentTargetsRunsInParallel() throws Exception {
    assertCanEnterTogether(CommandLockKey.of("host-a", "command"), CommandLockKey.of("host-b", "command"));
  }

  @Test
  public void entryIsRemovedOnlyAfterLastReferenceIsReleased() throws Exception {
    CommandLockManager manager = new CommandLockManager();
    CommandLockKey key = CommandLockKey.of("host-a", "shared command");
    CountDownLatch secondEntered = new CountDownLatch(1);
    CountDownLatch releaseSecond = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<?> second;
      try (LockHandle ignored = manager.acquire(key, LOCK_TIMEOUT)) {
        Assertions.assertThat(manager.trackedKeys()).isEqualTo(1);
        AtomicReference<Thread> waiterThread = new AtomicReference<>();
        second = executor.submit(() -> {
          waiterThread.set(Thread.currentThread());
          runUnderLock(manager, key, secondEntered, releaseSecond);
        });
        awaitTimedWaiting(awaitThread(waiterThread));
      }
      Assertions.assertThat(secondEntered.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
      Assertions.assertThat(manager.trackedKeys()).isEqualTo(1);

      releaseSecond.countDown();
      second.get(AWAIT_SECONDS, TimeUnit.SECONDS);
      Assertions.assertThat(manager.trackedKeys()).isZero();
    } finally {
      releaseSecond.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void waitingTimesOutWithTargetAndCommandAndKeepsOwnerEntry() throws Exception {
    CommandLockManager manager = new CommandLockManager();
    CommandLockKey key = CommandLockKey.of("host-a", "long running task");
    CountDownLatch lockHeld = new CountDownLatch(1);
    CountDownLatch releaseLock = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<?> owner = executor.submit(() -> runUnderLock(manager, key, lockHeld, releaseLock));
      Assertions.assertThat(lockHeld.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

      Assertions.assertThatThrownBy(() -> manager.acquire(key, Duration.ofMillis(1)))
          .isInstanceOf(CommandLockTimeoutException.class)
          .hasMessageContaining("host-a")
          .hasMessageContaining("long running task");
      Assertions.assertThat(manager.trackedKeys()).isEqualTo(1);

      releaseLock.countDown();
      owner.get(AWAIT_SECONDS, TimeUnit.SECONDS);
      Assertions.assertThat(manager.trackedKeys()).isZero();
    } finally {
      releaseLock.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void failingCommandReleasesItsKey() throws Exception {
    CommandLockManager manager = new CommandLockManager();
    CommandLockKey key = CommandLockKey.of("host-a", "failing command");

    Assertions.assertThatThrownBy(() -> {
      try (LockHandle ignored = manager.acquire(key, LOCK_TIMEOUT)) {
        throw new IllegalArgumentException("command failed");
      }
    }).isInstanceOf(IllegalArgumentException.class);

    Assertions.assertThat(manager.trackedKeys()).isZero();
    try (LockHandle handle = manager.acquire(key, LOCK_TIMEOUT)) {
      Assertions.assertThat(handle).isNotNull();
    }
  }

  @Test
  public void closingHandleTwiceReleasesOnce() throws Exception {
    CommandLockManager manager = new CommandLockManager();
    CommandLockKey key = CommandLockKey.of("host-a", "idempotent close");
    LockHandle handle = manager.acquire(key, LOCK_TIMEOUT);

    handle.close();
    handle.close();

    Assertions.assertThat(manager.trackedKeys()).isZero();
    try (LockHandle next = manager.acquire(key, LOCK_TIMEOUT)) {
      Assertions.assertThat(next).isNotNull();
      Assertions.assertThat(manager.trackedKeys()).isEqualTo(1);
    }
    Assertions.assertThat(manager.trackedKeys()).isZero();
  }

  @Test
  public void failedCloseFromNonOwnerThreadCanBeRetriedByOwner() throws Exception {
    CommandLockManager manager = new CommandLockManager();
    CommandLockKey key = CommandLockKey.of("host-a", "owner close");
    LockHandle handle = manager.acquire(key, LOCK_TIMEOUT);
    AtomicReference<Throwable> nonOwnerFailure = new AtomicReference<>();
    Thread nonOwner = new Thread(() -> {
      try {
        handle.close();
      } catch (RuntimeException e) {
        nonOwnerFailure.set(e);
      }
    });

    nonOwner.start();
    nonOwner.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS));

    Assertions.assertThat(nonOwner.isAlive()).isFalse();
    Assertions.assertThat(nonOwnerFailure.get()).isInstanceOf(IllegalMonitorStateException.class);
    Assertions.assertThat(manager.trackedKeys()).isEqualTo(1);
    handle.close();
    Assertions.assertThat(manager.trackedKeys()).isZero();
  }

  @Test
  public void interruptedWaiterPropagatesInterruptionAndReleasesItsReference() throws Exception {
    CommandLockManager manager = new CommandLockManager();
    CommandLockKey key = CommandLockKey.of("host-a", "interrupted command");
    AtomicBoolean waiterInterrupted = new AtomicBoolean();
    AtomicBoolean waiterAcquired = new AtomicBoolean();

    try (LockHandle ignored = manager.acquire(key, LOCK_TIMEOUT)) {
      Thread waiter = new Thread(() -> {
        try (LockHandle unused = manager.acquire(key, LOCK_TIMEOUT)) {
          waiterAcquired.set(true);
        } catch (InterruptedException e) {
          waiterInterrupted.set(true);
        }
      });
      waiter.start();
      awaitTimedWaiting(waiter);
      waiter.interrupt();
      waiter.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS));

      Assertions.assertThat(waiter.isAlive()).isFalse();
      Assertions.assertThat(waiterInterrupted).isTrue();
      Assertions.assertThat(waiterAcquired).isFalse();
      Assertions.assertThat(manager.trackedKeys()).isEqualTo(1);
    }
    Assertions.assertThat(manager.trackedKeys()).isZero();
  }

  @Test
  public void simultaneousCallersNeverRunSameKeyConcurrently() throws Exception {
    CommandLockManager manager = new CommandLockManager();
    CommandLockKey key = CommandLockKey.of("host-a", "shared command");
    int callerCount = 8;
    ExecutorService executor = Executors.newFixedThreadPool(callerCount);
    CyclicBarrier start = new CyclicBarrier(callerCount);
    AtomicInteger active = new AtomicInteger();
    AtomicInteger maximumActive = new AtomicInteger();
    AtomicInteger completed = new AtomicInteger();
    List<Future<?>> futures = new ArrayList<>();
    try {
      for (int index = 0; index < callerCount; index++) {
        futures.add(executor.submit(() -> {
          start.await();
          try (LockHandle ignored = manager.acquire(key, LOCK_TIMEOUT)) {
            maximumActive.accumulateAndGet(active.incrementAndGet(), Math::max);
            Thread.yield();
            active.decrementAndGet();
            completed.incrementAndGet();
          }
          return null;
        }));
      }
      for (Future<?> future : futures) {
        future.get(AWAIT_SECONDS, TimeUnit.SECONDS);
      }

      Assertions.assertThat(completed.get()).isEqualTo(callerCount);
      Assertions.assertThat(maximumActive.get()).isEqualTo(1);
      Assertions.assertThat(manager.trackedKeys()).isZero();
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void nullArgumentsAreRejected() {
    CommandLockManager manager = new CommandLockManager();
    CommandLockKey key = CommandLockKey.of("host-a", "command");

    Assertions.assertThatNullPointerException().isThrownBy(() -> CommandLockKey.of(null, "command"))
        .withMessage("target");
    Assertions.assertThatNullPointerException().isThrownBy(() -> CommandLockKey.of("host-a", null))
        .withMessage("command");
    Assertions.assertThatNullPointerException().isThrownBy(() -> manager.acquire(null, LOCK_TIMEOUT))
        .withMessage("key");
    Assertions.assertThatNullPointerException().isThrownBy(() -> manager.acquire(key, null))
        .withMessage("timeout");
    Assertions.assertThat(manager.trackedKeys()).isZero();
  }

  private static void assertCanEnterTogether(CommandLockKey firstKey, CommandLockKey secondKey) throws Exception {
    CommandLockManager manager = new CommandLockManager();
    CountDownLatch entered = new CountDownLatch(2);
    CountDownLatch release = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> first = executor.submit(() -> runUnderLock(manager, firstKey, entered, release));
      Future<?> second = executor.submit(() -> runUnderLock(manager, secondKey, entered, release));

      Assertions.assertThat(entered.await(AWAIT_SECONDS, TimeUnit.SECONDS))
          .as("both keys are held at the same time")
          .isTrue();
      Assertions.assertThat(manager.trackedKeys()).isEqualTo(2);

      release.countDown();
      first.get(AWAIT_SECONDS, TimeUnit.SECONDS);
      second.get(AWAIT_SECONDS, TimeUnit.SECONDS);
      Assertions.assertThat(manager.trackedKeys()).isZero();
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
  }

  private static void runUnderLock(CommandLockManager manager, CommandLockKey key, CountDownLatch entered,
      CountDownLatch release) {
    try (LockHandle ignored = manager.acquire(key, LOCK_TIMEOUT)) {
      entered.countDown();
      Assertions.assertThat(release.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  static Thread awaitThread(AtomicReference<Thread> reference) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(AWAIT_SECONDS);
    while (reference.get() == null && System.nanoTime() < deadline) {
      Thread.onSpinWait();
    }
    Assertions.assertThat(reference.get()).as("worker thread started").isNotNull();
    return reference.get();
  }

  static void awaitTimedWaiting(Thread thread) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(AWAIT_SECONDS);
    while (thread.getState() != Thread.State.TIMED_WAITING && System.nanoTime() < deadline) {
      Thread.onSpinWait();
    }
    Assertions.assertThat(thread.getState())
        .as("thread is parked waiting for the command lock")
        .isEqualTo(Thread.State.TIMED_WAITING);
  }
}
