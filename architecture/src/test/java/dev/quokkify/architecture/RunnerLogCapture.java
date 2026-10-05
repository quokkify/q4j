package dev.quokkify.architecture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Captures what {@link ArchitectureRunner} logs during a test and keeps it off the console.
 *
 * <p>Capturing the events, rather than the rendered text, lets a test assert the level a finding was
 * reported at. Keeping them off the console matters just as much: tests violate rules on purpose, so the
 * build output would otherwise carry WARN and ERROR report blocks that look like a failure.
 *
 * <p>Safe to use from tests running in parallel. A single appender is attached once and routes every event
 * to the {@link ThreadLocal} sink of the thread that logged it, so a capture only ever sees its own thread's
 * events. Additivity is a property of the shared logger, so it is reference counted: switched off by the
 * first open capture and restored by the last one to close.
 *
 * <p>Captures are not nestable on one thread: opening a second capture on the same thread replaces the sink
 * of the first. No test needs that today.
 */
public final class RunnerLogCapture implements AutoCloseable {

  private static final ThreadLocal<List<String>> SINK = new ThreadLocal<>();
  private static final AtomicInteger OPEN_CAPTURES = new AtomicInteger();
  private static final CapturingAppender APPENDER = install();

  private final List<String> events = Collections.synchronizedList(new ArrayList<>());

  private RunnerLogCapture() {
    SINK.set(events);
    if (OPEN_CAPTURES.incrementAndGet() == 1) {
      runnerLogger().setAdditive(false);
    }
  }

  /**
   * Starts capturing on the current thread, and stops the captured events from reaching the console.
   *
   * @return an open capture, to be closed by the caller
   */
  public static RunnerLogCapture open() {
    return new RunnerLogCapture();
  }

  /**
   * Returns the events captured on the opening thread, one per line, each prefixed with its level.
   *
   * @return captured output, empty when nothing was logged
   */
  public String output() {
    return String.join(System.lineSeparator(), List.copyOf(events));
  }

  /**
   * Clears this thread's sink and, once no capture is left open, restores console logging.
   */
  @Override
  public void close() {
    SINK.remove();
    if (OPEN_CAPTURES.decrementAndGet() == 0) {
      runnerLogger().setAdditive(true);
    }
  }

  private static CapturingAppender install() {
    CapturingAppender appender = new CapturingAppender();
    appender.start();
    runnerLogger().addAppender(appender);
    return appender;
  }

  private static Logger runnerLogger() {
    return (Logger) LogManager.getLogger(ArchitectureRunner.class);
  }

  /**
   * Routes each event to the sink of the thread that logged it, ignoring threads with no open capture.
   */
  private static final class CapturingAppender extends AbstractAppender {

    private CapturingAppender() {
      super("runner-log-capture", null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
    }

    /**
     * A synchronous appender is invoked on the thread that logged, so the thread local of the current
     * thread is the sink of the capture that thread opened, if any.
     */
    @Override
    public void append(LogEvent event) {
      List<String> sink = SINK.get();
      if (Objects.isNull(sink)) {
        return;
      }
      sink.add("%s %s".formatted(event.getLevel(), event.getMessage().getFormattedMessage()));
    }
  }
}
