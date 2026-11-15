package dev.quokkify.tms.listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import dev.quokkify.annotation.Sequential;
import dev.quokkify.tms.TestManagementSystem;
import dev.quokkify.tms.TmsConfigurationException;
import dev.quokkify.tms.TmsResult;
import dev.quokkify.tms.TmsStatus;

import io.qameta.allure.TmsLink;
import org.testng.IAnnotationTransformer;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.TestNG;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Runs TestNG programmatically to pin the TestNG behaviour {@link TmsListener} relies on.
 */
public class TmsListenerTestNgRunTest {

  private static final AtomicInteger SAMPLE_EXECUTIONS = new AtomicInteger();
  private static final AtomicInteger REPORTED_BEFORE_SECOND_CLASS = new AtomicInteger();
  private static final AtomicReference<RecordingSystem> FIRST_CLASS_SYSTEM = new AtomicReference<>();

  @BeforeMethod
  public void resetCounter() {
    SAMPLE_EXECUTIONS.set(0);
  }

  @TmsLink("TMS_LISTENER_RUN_1")
  @Sequential
  @Test(description = "Ambiguous providers abort the TestNG run before any test body executes")
  public void ambiguousProvidersAbortRunBeforeTests() {
    TestNG testNg = testNg(SampleTest.class);
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
  @Sequential
  @Test(description = "Two registered listener instances report a result only once")
  public void listenerRegisteredTwiceReportsOnce() {
    RecordingSystem system = new RecordingSystem();
    TestNG testNg = testNg(SampleTest.class);
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

  @TmsLink("TMS_LISTENER_RUN_3")
  @SingleThread
  @Test(description = "Regression guard: a passing last data provider row does not hide earlier failures")
  public void dataProviderRowsBecomeOneWorstStatusResult() {
    RecordingSystem system = new RecordingSystem();
    TestNG testNg = testNg(DataDrivenSampleTest.class);
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));

    testNg.run();

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.caseId()).isEqualTo("9");
      assertThat(result.status()).isEqualTo(TmsStatus.FAILED);
      assertThat(result.comment().lines()).first().isEqualTo("3 invocations: 2 failed, 1 passed");
      assertThat(result.comment()).contains("[1] FAILED (row-1, false): ", "[2] FAILED (row-2, false): ");
      assertThat(result.comment()).doesNotContain("row-3");
    });
  }

  @TmsLink("TMS_LISTENER_RUN_4")
  @SingleThread
  @Test(description = "A data provider with all rows passing is reported as one PASSED result")
  public void passingDataProviderRowsBecomeOnePassedResult() {
    RecordingSystem system = new RecordingSystem();
    TestNG testNg = testNg(PassingDataDrivenSampleTest.class);
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));

    testNg.run();

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.caseId()).isEqualTo("9");
      assertThat(result.status()).isEqualTo(TmsStatus.PASSED);
      assertThat(result.comment()).isEqualTo("3 invocations: 3 passed");
    });
  }

  @TmsLink("TMS_LISTENER_RUN_5")
  @SingleThread
  @Test(description = "Invocations of a method with invocationCount are reported as one FAILED result")
  public void invocationCountBecomesOneWorstStatusResult() {
    RecordingSystem system = new RecordingSystem();
    TestNG testNg = testNg(InvocationCountSampleTest.class);
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));

    testNg.run();

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.caseId()).isEqualTo("9");
      assertThat(result.status()).isEqualTo(TmsStatus.FAILED);
      assertThat(result.comment()).contains("[2] FAILED: ");
    });
  }

  @TmsLink("TMS_LISTENER_RUN_6")
  @SingleThread
  @Test(description = "Rows of a parallel data provider are reported as one result")
  public void parallelDataProviderRowsBecomeOneResult() {
    RecordingSystem system = new RecordingSystem();
    TestNG testNg = testNg(ParallelDataDrivenSampleTest.class);
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));

    testNg.run();

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.status()).isEqualTo(TmsStatus.FAILED);
      assertThat(result.comment().lines()).first().isEqualTo("20 invocations: 1 failed, 19 passed");
    });
  }

  @TmsLink("TMS_LISTENER_RUN_7")
  @SingleThread
  @Test(description = "A data-driven method linked to two cases yields one result per case")
  public void dataDrivenMethodYieldsOneResultPerCase() {
    RecordingSystem system = new RecordingSystem();
    TestNG testNg = testNg(TwoCasesDataDrivenSampleTest.class);
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));

    testNg.run();

    assertThat(system.reported).extracting(TmsResult::caseId).containsExactlyInAnyOrder("9", "10");
  }

  @TmsLink("TMS_LISTENER_RUN_8")
  @SingleThread
  @Test(description = "A plain method is still reported once as PASSED without a comment")
  public void plainMethodIsReportedOnce() {
    RecordingSystem system = new RecordingSystem();
    TestNG testNg = testNg(SampleTest.class);
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));

    testNg.run();

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.caseId()).isEqualTo("1");
      assertThat(result.status()).isEqualTo(TmsStatus.PASSED);
      assertThat(result.comment()).isNull();
    });
  }

  @TmsLink("TMS_LISTENER_RUN_9")
  @SingleThread
  @Test(description = "Results of a data-driven method are reported when the suite finishes, before execution finish")
  public void dataDrivenResultsAreReportedBeforeExecutionFinish() {
    RecordingSystem system = new RecordingSystem();
    AtomicInteger reportedAtSuiteFinish = new AtomicInteger(-1);
    TestNG testNg = testNg(DataDrivenSampleTest.class);
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));
    testNg.addListener(new ISuiteListener() {
      @Override
      public void onFinish(ISuite suite) {
        reportedAtSuiteFinish.set(system.reported.size());
      }
    });

    testNg.run();

    assertThat(reportedAtSuiteFinish).as("Results reported at suite finish").hasValue(1);
  }

  @TmsLink("TMS_LISTENER_RUN_10")
  @SingleThread
  @Test(description = "Regression guard: pooled invocationCount invocations are reported as one result")
  public void pooledInvocationCountBecomesOneResult() {
    RecordingSystem system = new RecordingSystem();
    TestNG testNg = testNg(PooledInvocationCountSampleTest.class);
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));

    testNg.run();

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.status()).isEqualTo(TmsStatus.FAILED);
      assertThat(result.comment().lines()).first().isEqualTo("3 invocations: 1 failed, 2 passed");
    });
  }

  @TmsLink("TMS_LISTENER_RUN_11")
  @SingleThread
  @Test(description = "Regression guard: a parameter with a throwing toString does not break reporting")
  public void throwingParameterDoesNotBreakReporting() {
    RecordingSystem system = new RecordingSystem();
    TestNG testNg = testNg(ThrowingParameterSampleTest.class);
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));

    testNg.run();

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.status()).isEqualTo(TmsStatus.FAILED);
      assertThat(result.comment().lines()).first().isEqualTo("1 invocation: 1 failed");
      assertThat(result.comment()).contains("[1] FAILED");
    });
    assertThat(system.finished).as("Run finished").hasValue(1);
  }

  @TmsLink("TMS_LISTENER_RUN_12")
  @SingleThread
  @Test(description = "Regression guard: results of a repeated method are reported when its class finishes")
  public void repeatedResultsAreReportedWhenClassFinishes() {
    RecordingSystem system = new RecordingSystem();
    FIRST_CLASS_SYSTEM.set(system);
    REPORTED_BEFORE_SECOND_CLASS.set(-1);
    TestNG testNg = testNg(DataDrivenSampleTest.class, AfterDataDrivenSampleTest.class);
    testNg.addListener(new TmsListener(() -> Optional.of(system), List::of));

    testNg.run();

    assertThat(REPORTED_BEFORE_SECOND_CLASS).as("Results reported before the second class").hasValue(1);
    assertThat(system.reported).hasSize(1);
  }

  private static TestNG testNg(Class<?>... testClasses) {
    TestNG testNg = new TestNG(false);
    testNg.setVerbose(0);
    // keep the nested run free of the classpath ServiceLoader listeners (SuiteListener would regenerate its suite)
    testNg.setServiceLoaderClassLoader(new URLClassLoader(new URL[0], null));
    testNg.setTestClasses(testClasses);
    testNg.addListener(new IAnnotationTransformer() {
      @Override
      public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor,
          Method testMethod) {
        if (Objects.nonNull(testMethod) && List.of(testClasses).contains(testMethod.getDeclaringClass())) {
          annotation.setEnabled(true);
        }
      }
    });
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

  // The sample classes below are disabled so the outer run skips them; testNg enables them for the nested run only.
  private static final class DataDrivenSampleTest {

    @DataProvider
    public Object[][] rows() {
      return new Object[][] {{"row-1", false}, {"row-2", false}, {"row-3", true}};
    }

    @TmsLink("9")
    @Test(enabled = false, dataProvider = "rows")
    public void parameterized(String row, boolean passes) {
      assertThat(passes).as(row).isTrue();
    }
  }

  private static final class PassingDataDrivenSampleTest {

    @DataProvider
    public Object[][] rows() {
      return new Object[][] {{"row-1"}, {"row-2"}, {"row-3"}};
    }

    @TmsLink("9")
    @Test(enabled = false, dataProvider = "rows")
    public void parameterized(String row) {
      assertThat(row).isNotEmpty();
    }
  }

  private static final class InvocationCountSampleTest {

    private final AtomicInteger invocations = new AtomicInteger();

    @TmsLink("9")
    @Test(enabled = false, invocationCount = 3)
    public void repeated() {
      assertThat(invocations.incrementAndGet()).as("invocation").isNotEqualTo(2);
    }
  }

  private static final class ParallelDataDrivenSampleTest {

    @DataProvider(parallel = true)
    public Object[][] rows() {
      Object[][] rows = new Object[20][];
      for (int i = 0; i < rows.length; i++) {
        rows[i] = new Object[] {i};
      }
      return rows;
    }

    @TmsLink("9")
    @Test(enabled = false, dataProvider = "rows")
    public void parameterized(int row) {
      assertThat(row).isNotEqualTo(7);
    }
  }

  private static final class TwoCasesDataDrivenSampleTest {

    @DataProvider
    public Object[][] rows() {
      return new Object[][] {{"row-1"}, {"row-2"}};
    }

    @TmsLink("9")
    @TmsLink("10")
    @Test(enabled = false, dataProvider = "rows")
    public void parameterized(String row) {
      assertThat(row).isNotEmpty();
    }
  }

  private static final class PooledInvocationCountSampleTest {

    private final AtomicInteger invocations = new AtomicInteger();

    @TmsLink("9")
    @Test(enabled = false, invocationCount = 3, threadPoolSize = 3)
    public void repeated() {
      assertThat(invocations.incrementAndGet()).as("invocation").isNotEqualTo(2);
    }
  }

  private static final class ThrowingParameterSampleTest {

    @DataProvider
    public Object[][] rows() {
      return new Object[][] {{new ThrowingToString()}};
    }

    @TmsLink("9")
    @Test(enabled = false, dataProvider = "rows")
    public void parameterized(Object row) {
      assertThat(1).as("row").isEqualTo(2);
    }
  }

  private static final class ThrowingToString {

    @Override
    public String toString() {
      throw new IllegalStateException("no string for you");
    }
  }

  private static final class AfterDataDrivenSampleTest {

    @Test(enabled = false)
    public void records() {
      REPORTED_BEFORE_SECOND_CLASS.set(FIRST_CLASS_SYSTEM.get().reported.size());
    }
  }

  private static final class RecordingSystem implements TestManagementSystem {

    private final List<TmsResult> reported = new CopyOnWriteArrayList<>();
    private final AtomicInteger finished = new AtomicInteger();

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

    @Override
    public void finishRun() {
      finished.incrementAndGet();
    }
  }
}
