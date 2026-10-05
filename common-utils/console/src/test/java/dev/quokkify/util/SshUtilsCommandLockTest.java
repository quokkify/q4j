package dev.quokkify.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.jcabi.ssh.Shell;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class SshUtilsCommandLockTest {

  private static final Duration QUEUE_TIMEOUT = Duration.ofSeconds(5);
  private static final long AWAIT_SECONDS = 5;

  @Test
  public void returnsCommandOutput() {
    Shell shell = new BlockingShell(new CountDownLatch(0), new CountDownLatch(0));

    String output = SshUtils.executeCommand(shell, "host-a", "echo ok", QUEUE_TIMEOUT);

    Assertions.assertThat(output).isEqualTo("echo ok");
  }

  @Test
  public void identicalCommandOnSameTargetWaitsForRunningOne() throws Exception {
    String command = "deploy " + System.nanoTime();
    CountDownLatch firstStarted = new CountDownLatch(1);
    CountDownLatch releaseFirst = new CountDownLatch(1);
    CountDownLatch secondStarted = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<String> first = executor.submit(() -> SshUtils.executeCommand(
          new BlockingShell(firstStarted, releaseFirst), "host-a", command, QUEUE_TIMEOUT));
      Assertions.assertThat(firstStarted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
      AtomicReference<Thread> waiterThread = new AtomicReference<>();
      Future<String> second = executor.submit(() -> {
        waiterThread.set(Thread.currentThread());
        return SshUtils.executeCommand(
            new BlockingShell(secondStarted, new CountDownLatch(0)), "host-a", command, QUEUE_TIMEOUT);
      });
      CommandLockManagerTest.awaitTimedWaiting(CommandLockManagerTest.awaitThread(waiterThread));
      Assertions.assertThat(secondStarted.getCount()).isEqualTo(1);

      releaseFirst.countDown();

      Assertions.assertThat(first.get(AWAIT_SECONDS, TimeUnit.SECONDS)).isEqualTo(command);
      Assertions.assertThat(second.get(AWAIT_SECONDS, TimeUnit.SECONDS)).isEqualTo(command);
    } finally {
      releaseFirst.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void identicalCommandOnDifferentTargetsRunsInParallel() throws Exception {
    String command = "deploy " + System.nanoTime();
    CountDownLatch started = new CountDownLatch(2);
    CountDownLatch release = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<String> first = executor.submit(() -> SshUtils.executeCommand(
          new BlockingShell(started, release), "host-a", command, QUEUE_TIMEOUT));
      Future<String> second = executor.submit(() -> SshUtils.executeCommand(
          new BlockingShell(started, release), "host-b", command, QUEUE_TIMEOUT));

      Assertions.assertThat(started.await(AWAIT_SECONDS, TimeUnit.SECONDS))
          .as("both commands run at the same time")
          .isTrue();

      release.countDown();
      first.get(AWAIT_SECONDS, TimeUnit.SECONDS);
      second.get(AWAIT_SECONDS, TimeUnit.SECONDS);
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void waiterTimesOutWhileIdenticalCommandRuns() throws Exception {
    String command = "deploy " + System.nanoTime();
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<String> running = executor.submit(() -> SshUtils.executeCommand(
          new BlockingShell(started, release), "host-a", command, QUEUE_TIMEOUT));
      Assertions.assertThat(started.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

      Assertions.assertThatThrownBy(() -> SshUtils.executeCommand(
              new BlockingShell(new CountDownLatch(0), new CountDownLatch(0)), "host-a", command,
              Duration.ofMillis(1)))
          .isInstanceOf(CommandLockTimeoutException.class)
          .hasMessageContaining("host-a")
          .hasMessageContaining(command);

      release.countDown();
      running.get(AWAIT_SECONDS, TimeUnit.SECONDS);
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void interruptedWaiterFailsAndKeepsInterruptFlag() throws Exception {
    String command = "deploy " + System.nanoTime();
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    AtomicBoolean interruptFlag = new AtomicBoolean();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<String> running = executor.submit(() -> SshUtils.executeCommand(
          new BlockingShell(started, release), "host-a", command, QUEUE_TIMEOUT));
      Assertions.assertThat(started.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
      Thread waiter = new Thread(() -> {
        try {
          SshUtils.executeCommand(new BlockingShell(new CountDownLatch(0), new CountDownLatch(0)),
              "host-a", command, QUEUE_TIMEOUT);
        } catch (IllegalStateException e) {
          failure.set(e);
          interruptFlag.set(Thread.currentThread().isInterrupted());
        }
      });
      waiter.start();
      CommandLockManagerTest.awaitTimedWaiting(waiter);
      waiter.interrupt();
      waiter.join(TimeUnit.SECONDS.toMillis(AWAIT_SECONDS));

      Assertions.assertThat(waiter.isAlive()).isFalse();
      Assertions.assertThat(failure.get())
          .isInstanceOf(IllegalStateException.class)
          .hasCauseInstanceOf(InterruptedException.class)
          .hasMessageContaining("host-a");
      Assertions.assertThat(interruptFlag).isTrue();

      release.countDown();
      running.get(AWAIT_SECONDS, TimeUnit.SECONDS);
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  public void nullArgumentsAreRejected() {
    Shell shell = new BlockingShell(new CountDownLatch(0), new CountDownLatch(0));

    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> SshUtils.executeCommand(null, "host-a", "echo", QUEUE_TIMEOUT))
        .withMessage("shell");
    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> SshUtils.executeCommand(shell, null, "echo", QUEUE_TIMEOUT))
        .withMessage("target");
    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> SshUtils.executeCommand(shell, "host-a", null, QUEUE_TIMEOUT))
        .withMessage("command");
    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> SshUtils.executeCommand(shell, "host-a", "echo", null))
        .withMessage("timeout");
  }

  private static final class BlockingShell implements Shell {

    private final CountDownLatch started;
    private final CountDownLatch release;

    private BlockingShell(CountDownLatch started, CountDownLatch release) {
      this.started = started;
      this.release = release;
    }

    @Override
    public int exec(String command, InputStream stdin, OutputStream stdout, OutputStream stderr) {
      started.countDown();
      try {
        if (!release.await(AWAIT_SECONDS, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Test shell was not released");
        }
        stdout.write(command.getBytes(StandardCharsets.UTF_8));
        return 0;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      } catch (java.io.IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
