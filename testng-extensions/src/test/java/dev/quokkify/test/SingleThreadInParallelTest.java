package dev.quokkify.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.config.TestNGExtension;
import dev.quokkify.listener.lifecycle.SuiteListener;

import org.assertj.core.api.Assertions;
import org.testng.IAlterSuiteListener;
import org.testng.IAnnotationTransformer;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.TestNG;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

public class SingleThreadInParallelTest {

  private static final String OPTION = "SINGLE_THREAD_TESTS_IN_PARALLEL";
  private static final String PROBE = "single-thread-probe";
  private static final long OVERLAP_TIMEOUT_SECONDS = 30;

  @SingleThread
  @Test(description = "Single thread tests start after the concurrent tests by default")
  public void testSingleThreadTestsRunAfterConcurrentTestsByDefault() {
    Probe probe = new Probe(false);
    XmlSuite.ParallelMode suiteMode = runWithOption(null, probe);

    Assertions.assertThat(suiteMode).as("Generated suite parallel mode").isEqualTo(XmlSuite.ParallelMode.NONE);
    Assertions.assertThat(probe.events.indexOf("concurrent:end"))
        .as("Concurrent test finished before the first single thread test started: %s", probe.events)
        .isNotNegative()
        .isLessThan(probe.events.indexOf("single:start"));
    assertSingleThreadTestsRanOneByOne(probe);
  }

  @SingleThread
  @Test(description = "Single thread tests run alongside the concurrent tests when the option is enabled")
  public void testSingleThreadTestsRunAlongsideConcurrentTestsWhenEnabled() {
    Probe probe = new Probe(true);
    XmlSuite.ParallelMode suiteMode = runWithOption("true", probe);

    Assertions.assertThat(suiteMode).as("Generated suite parallel mode").isEqualTo(XmlSuite.ParallelMode.TESTS);
    Assertions.assertThat(probe.overlapObserved.get())
        .as("Concurrent test observed a single thread test while still running: %s", probe.events)
        .isTrue();
    Assertions.assertThat(probe.singleThreadNames)
        .as("Single thread tests ran outside the concurrent test thread")
        .doesNotContainAnyElementsOf(probe.concurrentThreadNames);
    assertSingleThreadTestsRanOneByOne(probe);
  }

  private static void assertSingleThreadTestsRanOneByOne(Probe probe) {
    Assertions.assertThat(probe.events).as("Probe events").filteredOn("single:start"::equals).hasSize(2);
    Assertions.assertThat(probe.maxActiveSingleThreadTests.get()).as("Max simultaneous single thread tests").isEqualTo(1);
    Assertions.assertThat(probe.singleThreadNames).as("Single thread test threads").hasSize(1);
  }

  private static XmlSuite.ParallelMode runWithOption(String value, Probe probe) {
    TestNGExtension config = ConfigRegistry.getMutable(TestNGExtension.class);
    String previous = Objects.isNull(value) ? config.removeProperty(OPTION) : config.setProperty(OPTION, value);
    try {
      return runFixtureSuite(probe);
    } finally {
      if (Objects.isNull(previous)) {
        config.removeProperty(OPTION);
      } else {
        config.setProperty(OPTION, previous);
      }
    }
  }

  private static XmlSuite.ParallelMode runFixtureSuite(Probe probe) {
    XmlSuite suite = new XmlSuite();
    suite.setName("Single thread fixture");
    XmlTest test = new XmlTest(suite);
    test.setName("Fixture");
    test.setXmlClasses(List.of(new XmlClass(FixtureTest.class)));
    AtomicReference<XmlSuite.ParallelMode> suiteMode = new AtomicReference<>();
    TestNG testng = new TestNG(false);
    testng.setUseDefaultListeners(false);
    testng.setServiceLoaderClassLoader(new URLClassLoader(new URL[0], null));
    testng.addListener(new IAlterSuiteListener() {
      @Override
      public void alter(List<XmlSuite> suites) {
        new SuiteListener().alter(suites);
      }
    });
    testng.addListener(new ISuiteListener() {
      @Override
      public void onStart(ISuite runningSuite) {
        suiteMode.set(runningSuite.getXmlSuite().getParallel());
        runningSuite.setAttribute(PROBE, probe);
      }
    });
    testng.addListener(new IAnnotationTransformer() {
      @Override
      public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        if (Objects.nonNull(testMethod) && FixtureTest.class.equals(testMethod.getDeclaringClass())) {
          annotation.setEnabled(true);
        }
      }
    });
    testng.setXmlSuites(new ArrayList<>(List.of(suite)));
    testng.run();
    Assertions.assertThat(testng.getStatus()).as("Fixture suite status, events: %s", probe.events).isZero();
    return suiteMode.get();
  }

  // Disabled so the outer run skips it; runFixtureSuite enables it for the nested TestNG run only.
  public static class FixtureTest {

    @Test(enabled = false)
    public void concurrent(ITestContext context) throws InterruptedException {
      Probe probe = (Probe) context.getSuite().getAttribute(PROBE);
      probe.events.add("concurrent:start");
      probe.concurrentThreadNames.add(Thread.currentThread().getName());
      if (probe.awaitSingleThreadTest) {
        probe.overlapObserved.set(probe.singleThreadStarted.await(OVERLAP_TIMEOUT_SECONDS, TimeUnit.SECONDS));
      }
      probe.events.add("concurrent:end");
    }

    @SingleThread
    @Test(enabled = false)
    public void firstSingleThread(ITestContext context) {
      runSingleThread(context);
    }

    @SingleThread
    @Test(enabled = false)
    public void secondSingleThread(ITestContext context) {
      runSingleThread(context);
    }

    private void runSingleThread(ITestContext context) {
      Probe probe = (Probe) context.getSuite().getAttribute(PROBE);
      probe.events.add("single:start");
      probe.singleThreadNames.add(Thread.currentThread().getName());
      probe.maxActiveSingleThreadTests.accumulateAndGet(probe.activeSingleThreadTests.incrementAndGet(), Math::max);
      probe.singleThreadStarted.countDown();
      probe.activeSingleThreadTests.decrementAndGet();
      probe.events.add("single:end");
    }
  }

  private static final class Probe {

    private final boolean awaitSingleThreadTest;
    private final List<String> events = new CopyOnWriteArrayList<>();
    private final Set<String> concurrentThreadNames = ConcurrentHashMap.newKeySet();
    private final Set<String> singleThreadNames = ConcurrentHashMap.newKeySet();
    private final CountDownLatch singleThreadStarted = new CountDownLatch(1);
    private final AtomicInteger activeSingleThreadTests = new AtomicInteger();
    private final AtomicInteger maxActiveSingleThreadTests = new AtomicInteger();
    private final AtomicBoolean overlapObserved = new AtomicBoolean();

    private Probe(boolean awaitSingleThreadTest) {
      this.awaitSingleThreadTest = awaitSingleThreadTest;
    }
  }
}
