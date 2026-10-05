package dev.quokkify.architecture.contract;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that {@link ModelClock} separates model build time from the time rules spend on their own work.
 */
public class ModelClockTest {

  private static final long OUTER_MILLIS = 60;
  private static final long NESTED_MILLIS = 120;
  private static final long TOLERANCE_MILLIS = 5;
  private static final int TIMEOUT_SECONDS = 10;

  @Test
  public void nestedModelIsChargedToItsOwnBuildOnly() {
    ModelClock clock = new ModelClock();

    clock.build("outer", () -> {
      sleep(OUTER_MILLIS);
      return clock.access(() -> clock.build("nested", () -> {
        sleep(NESTED_MILLIS);
        return "model";
      }));
    });

    List<ModelClock.ModelTime> models = clock.builtModels();
    assertThat(models).extracting(ModelClock.ModelTime::name).containsExactly("nested", "outer");
    assertThat(models.get(0).millis()).isGreaterThanOrEqualTo(NESTED_MILLIS - TOLERANCE_MILLIS);
    assertThat(models.get(1).millis())
        .as("the outer build must not include the nested one")
        .isBetween(OUTER_MILLIS - TOLERANCE_MILLIS, OUTER_MILLIS + NESTED_MILLIS - 1);
  }

  @Test
  public void waitForAModelAnotherThreadBuildsIsNotChargedToTheWaitingBuild() throws InterruptedException {
    ModelClock clock = new ModelClock();
    Object lock = new Object();
    CountDownLatch parsing = new CountDownLatch(1);

    Thread parser = Thread.ofVirtual().start(() -> clock.access(() -> {
      synchronized (lock) {
        parsing.countDown();
        return clock.build("sources", () -> {
          sleep(NESTED_MILLIS);
          return "units";
        });
      }
    }));
    assertThat(parsing.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    clock.build("classes", () -> clock.access(() -> {
      synchronized (lock) {
        return "units";
      }
    }));
    parser.join();

    assertThat(clock.builtModels())
        .filteredOn(model -> model.name().equals("classes"))
        .singleElement()
        .satisfies(classes -> assertThat(classes.millis())
            .as("waiting for the sources another thread parses belongs to the sources only")
            .isLessThan(NESTED_MILLIS / 2));
  }

  @Test
  public void failedBuildIsNotReportedAsBuilt() {
    ModelClock clock = new ModelClock();

    assertThatThrownBy(() -> clock.access(() -> clock.build("broken", () -> {
      throw new IllegalStateException("cannot import");
    }))).isInstanceOf(IllegalStateException.class);

    assertThat(clock.builtModels()).isEmpty();
    assertThat(clock.access(() -> clock.build("retried", () -> "model"))).isEqualTo("model");
    assertThat(clock.builtModels()).extracting(ModelClock.ModelTime::name).containsExactly("retried");
  }

  @Test
  public void accessTimeIsCountedOncePerThreadAndThenReset() {
    ModelClock clock = new ModelClock();

    clock.access(() -> clock.access(() -> {
      sleep(OUTER_MILLIS);
      return "model";
    }));

    long spent = clock.takeAccessNanos() / 1_000_000;
    assertThat(spent)
        .as("a nested accessor must not count the same wait twice")
        .isBetween(OUTER_MILLIS - TOLERANCE_MILLIS, 2 * OUTER_MILLIS - 1);
    assertThat(clock.takeAccessNanos()).isZero();
  }

  @Test
  public void accessTimeIsKeptPerThread() throws InterruptedException {
    ModelClock clock = new ModelClock();
    AtomicLong otherThreadSpent = new AtomicLong();

    Thread other = Thread.ofVirtual().start(() -> {
      clock.access(() -> {
        sleep(OUTER_MILLIS);
        return "model";
      });
      otherThreadSpent.set(clock.takeAccessNanos());
    });
    other.join();

    assertThat(otherThreadSpent.get() / 1_000_000).isGreaterThanOrEqualTo(OUTER_MILLIS - TOLERANCE_MILLIS);
    assertThat(clock.takeAccessNanos()).as("another thread's access is not this thread's").isZero();
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(interrupted);
    }
  }
}
