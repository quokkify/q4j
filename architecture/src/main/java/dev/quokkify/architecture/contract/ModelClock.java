package dev.quokkify.architecture.contract;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Measures how long the shared models of an {@link ArchitectureContext} take to build, and how long each rule
 * thread spends obtaining them.
 *
 * <p>Rules run concurrently and share lazily built models, so the first rule to need a model builds it while
 * the others wait. Without this clock that wait would be charged to every waiting rule. The runner reports the
 * build time of each model once and charges a rule only with its own work.
 *
 * <p>A build that obtains another model, the ArchUnit import parsing the Java sources for instance, is charged
 * without it, whether it builds that model or waits for another thread to, so the reported times do not overlap.
 * Access time is tracked per thread: a rule that obtains a model from a thread of its own, a parallel stream for
 * example, has that time counted as its own work.
 */
public final class ModelClock {

  private final List<ModelTime> built = new ArrayList<>();
  private final ThreadLocal<Frame> frame = ThreadLocal.withInitial(Frame::new);

  ModelClock() {
  }

  /**
   * Runs a model accessor, adding the time it takes, waiting for a lock included, to the current thread.
   * Only the outermost accessor of a thread counts, so an accessor calling another is not counted twice. An
   * accessor called while a model is being built is excluded from that build, whether it builds a nested model
   * or waits for one another thread is building, so the reported build times never overlap.
   */
  <T> T access(Supplier<T> accessor) {
    Frame current = frame.get();
    boolean outermost = current.depth == 0;
    boolean insideBuild = current.buildDepth > 0;
    current.depth++;
    long startedAt = System.nanoTime();
    try {
      return accessor.get();
    } finally {
      current.depth--;
      long elapsed = System.nanoTime() - startedAt;
      if (outermost) {
        current.accessNanos += elapsed;
      }
      if (insideBuild) {
        current.excludedNanos += elapsed;
      }
    }
  }

  /**
   * Builds a model and records its build time, without the accessors called during the build. A build that
   * fails is not recorded: the model was not built, and the next accessor retries it.
   */
  <T> T build(String model, Supplier<T> builder) {
    Frame current = frame.get();
    long excludedBefore = current.excludedNanos;
    current.buildDepth++;
    long startedAt = System.nanoTime();
    boolean succeeded = false;
    try {
      T value = builder.get();
      succeeded = true;
      return value;
    } finally {
      long own = System.nanoTime() - startedAt - (current.excludedNanos - excludedBefore);
      current.buildDepth--;
      current.excludedNanos = excludedBefore;
      if (succeeded) {
        record(new ModelTime(model, Math.max(0, own) / 1_000_000));
      }
    }
  }

  private void record(ModelTime time) {
    synchronized (built) {
      built.add(time);
    }
  }

  /**
   * Returns the time the current thread spent obtaining models since the last call, and starts counting anew.
   *
   * <p>The count is confined to the calling thread: call it from the thread that did the work, before and
   * after it, to tell the work apart from obtaining models.
   *
   * @return nanoseconds spent in model accessors, building or waiting for a model included
   */
  public long takeAccessNanos() {
    Frame current = frame.get();
    long spent = current.accessNanos;
    current.accessNanos = 0;
    return spent;
  }

  /**
   * Returns the models built so far, slowest first.
   *
   * @return build time of each model built in this context
   */
  public List<ModelTime> builtModels() {
    synchronized (built) {
      return built.stream()
          .sorted(Comparator.comparingLong(ModelTime::millis).reversed().thenComparing(ModelTime::name))
          .toList();
    }
  }

  /**
   * Build time of one shared model.
   *
   * @param name   model name, for example {@code ArchUnit classes}
   * @param millis build time without nested builds and waits, in milliseconds
   */
  public record ModelTime(String name, long millis) {
  }

  private static final class Frame {
    private int depth;
    private int buildDepth;
    private long accessNanos;
    private long excludedNanos;
  }
}
