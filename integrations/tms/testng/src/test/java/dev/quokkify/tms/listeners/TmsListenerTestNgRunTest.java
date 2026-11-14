package dev.quokkify.tms.listeners;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.tms.TestManagementSystem;
import dev.quokkify.tms.TmsConfigurationException;
import dev.quokkify.tms.TmsResult;
import dev.quokkify.tms.TmsStatus;

import io.qameta.allure.TmsLink;
import org.testng.TestNG;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Runs TestNG programmatically to pin the TestNG behaviour {@link TmsListener} relies on.
 */
public class TmsListenerTestNgRunTest {

  private static final AtomicInteger SAMPLE_EXECUTIONS = new AtomicInteger();

  @BeforeMethod
  public void resetCounter() {
    SAMPLE_EXECUTIONS.set(0);
  }

  @TmsLink("TMS_LISTENER_RUN_1")
  @SingleThread
  @Test(description = "Ambiguous providers abort the TestNG run before any test body executes")
  public void ambiguousProvidersAbortRunBeforeTests() {
    TestNG testNg = testNg();
    testNg.addListener(new TmsListener(() -> {
      throw new TmsConfigurationException("Expected exactly one TMS provider on the classpath");
    }, List::of));

    Throwable thrown = catchThrowable(testNg::run);

    assertThat(thrown).as("Exception thrown by TestNG.run(), status=%s", testNg.getStatus())
        .isInstanceOf(TmsConfigurationException.class);
    assertThat(SAMPLE_EXECUTIONS).as("Sample test executions").hasValue(0);
    assertThat(TmsListener.isActive()).as("Listener active after aborted run").isFalse();
  }

  @TmsLink("TMS_LISTENER_RUN_2")
  @SingleThread
  @Test(description = "Two registered listener instances report a result only once")
  public void listenerRegisteredTwiceReportsOnce() {
    RecordingSystem system = new RecordingSystem();
    TestNG testNg = testNg();
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));

    testNg.run();

    assertThat(SAMPLE_EXECUTIONS).as("Sample test executions").hasValue(1);
    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.caseId()).isEqualTo("1");
      assertThat(result.status()).isEqualTo(TmsStatus.PASSED);
    });
    assertThat(TmsListener.isActive()).as("Listener active after run").isFalse();
  }

  private static TestNG testNg() {
    TestNG testNg = new TestNG(false);
    testNg.setVerbose(0);
    // keep the nested run free of the classpath ServiceLoader listeners (SuiteListener would regenerate its suite)
    testNg.setServiceLoaderClassLoader(new URLClassLoader(new URL[0], null));
    testNg.setTestClasses(new Class<?>[] {SampleTest.class});
    return testNg;
  }

  /**
   * Executed only by the programmatic TestNG runs above.
   */
  private static final class SampleTest {

    @TmsLink("1")
    @Test
    public void sample() {
      SAMPLE_EXECUTIONS.incrementAndGet();
    }
  }

  private static final class RecordingSystem implements TestManagementSystem {

    private final List<TmsResult> reported = new CopyOnWriteArrayList<>();

    @Override
    public String id() {
      return "recording";
    }

    @Override
    public boolean isConfigured() {
      return true;
    }

    @Override
    public void report(TmsResult result) {
      reported.add(result);
    }
  }
}
