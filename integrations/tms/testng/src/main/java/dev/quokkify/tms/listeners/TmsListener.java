package dev.quokkify.tms.listeners;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import dev.quokkify.tms.TestManagementSystem;
import dev.quokkify.tms.TestManagementSystems;
import dev.quokkify.tms.TmsResult;
import dev.quokkify.tms.TmsStatus;
import dev.quokkify.tms.tickets.TicketComments;
import dev.quokkify.tms.tickets.TicketSource;
import dev.quokkify.util.TestUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IClassListener;
import org.testng.IExecutionListener;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

/**
 * Single entry point of the test management system (TMS) integration.
 * Reports TestNG results to the only TMS provider on the classpath; case ids come from {@code @TmsLink}.
 * Methods that run repeatedly (data provider rows, {@code invocationCount}) produce one result per case, with the
 * worst status of their invocations, when their test class finishes; the end of the {@code <test>} and of the
 * execution are fallbacks. Invocations are not collected while the run is unavailable.
 * Tests without {@code @TmsLink} are executed but not reported. Reporting errors are logged and never fail the tests.
 * The provider is resolved once in {@link #onExecutionStart()}; a {@code TmsConfigurationException} aborts the run.
 *
 * <p>TestNG behaviour this class relies on, proven for TestNG 7.12.0 by {@code TmsListenerTestNgRunTest}
 * (the dedup only for registration through {@code TestNG.addListener}):
 * <ul>
 *     <li>an exception thrown by {@code onExecutionStart} propagates out of {@code TestNG.run()} before any test
 *     executes, so resolution needs no fallback in {@code intercept};</li>
 *     <li>TestNG keeps one listener instance per class, so a second {@code TmsListener} instance registered in the
 *     same run is never called and needs no duplicate guard.</li>
 * </ul>
 */
public class TmsListener implements IExecutionListener, ISuiteListener, ITestListener, IClassListener, IMethodInterceptor {

  private static final Logger LOG = LogManager.getLogger(TmsListener.class);
  private static final String NO_ERROR_MESSAGE = "Error message not found";
  private static final AtomicInteger ACTIVE_INSTANCES = new AtomicInteger();

  private final Supplier<Optional<TestManagementSystem>> resolver;
  private final Supplier<List<TicketSource>> tickets;
  private final InvocationCollector collector = new InvocationCollector();
  private volatile Optional<TestManagementSystem> system = Optional.empty();
  private volatile boolean started;
  private volatile boolean runAvailable = true;
  private Map<String, String> ticketComments;

  public TmsListener() {
    this(TestManagementSystems::resolve, TicketComments::load);
  }

  TmsListener(Supplier<Optional<TestManagementSystem>> resolver, Supplier<List<TicketSource>> tickets) {
    this.resolver = resolver;
    this.tickets = tickets;
  }

  /**
   * @return {@code true} from the start of a TestNG execution with a {@code TmsListener} until its finish
   */
  public static boolean isActive() {
    return ACTIVE_INSTANCES.get() > 0;
  }

  @Override
  public synchronized void onExecutionStart() {
    if (started) {
      return;
    }
    system = resolver.get();
    ticketComments = null;
    started = true;
    ACTIVE_INSTANCES.incrementAndGet();
  }

  @Override
  public synchronized void onExecutionFinish() {
    if (!started) {
      return;
    }
    try {
      system.ifPresent(tms -> {
        reportCollected(tms, collector::drainAll);
        try {
          tms.finishRun();
        } catch (RuntimeException e) {
          LOG.error("Can not finish the run in '{}': {}", tms.id(), e.getMessage(), e);
        }
      });
    } finally {
      started = false;
      ACTIVE_INSTANCES.decrementAndGet();
    }
  }

  @Override
  public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
    return system
        .map(tms -> methods.stream()
            .filter(method -> shouldRun(tms, TestUtils.getTmsLinks(method.getMethod())))
            .toList())
        .orElse(methods);
  }

  @Override
  public void onStart(ISuite suite) {
    startRun(suite.getAllMethods());
  }

  @Override
  public void onFinish(ITestContext context) {
    system.ifPresent(tms -> reportCollected(tms, () -> collector.drain(List.of(context.getAllTestMethods()))));
  }

  @Override
  public void onAfterClass(ITestClass testClass) {
    system.ifPresent(tms -> reportCollected(tms, () -> collector.drain(List.of(testClass.getTestMethods()))));
  }

  @Override
  public void onFinish(ISuite suite) {
    reportDisabled(suite.getExcludedMethods());
  }

  void startRun(Collection<ITestNGMethod> methods) {
    system.ifPresent(tms -> {
      Set<String> caseIds = methods.stream()
          .flatMap(method -> TestUtils.getTmsLinks(method).stream())
          .collect(Collectors.toCollection(LinkedHashSet::new));
      try {
        tms.startRun(caseIds);
        runAvailable = true;
      } catch (RuntimeException e) {
        runAvailable = false;
        LOG.error("Can not start a run in '{}', results will not be reported: {}", tms.id(), e.getMessage(), e);
      }
    });
  }

  void reportDisabled(Collection<ITestNGMethod> excludedMethods) {
    system.ifPresent(tms -> {
      Map<String, String> comments = ticketComments();
      excludedMethods.forEach(method -> TestUtils.getTmsLinks(method).stream()
          .filter(caseId -> !method.getEnabled() || comments.containsKey(caseId))
          .forEach(caseId -> report(tms, new TmsResult(
              caseId,
              TmsStatus.BLOCKED,
              comments.getOrDefault(caseId, "%s is disabled via 'enabled' attribute.".formatted(caseId)),
              null))));
    });
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    reportResult(result, TmsStatus.PASSED, null);
  }

  @Override
  public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    reportResult(result, TmsStatus.PASSED, errorMessage(result));
  }

  @Override
  public void onTestFailure(ITestResult result) {
    reportResult(result, TmsStatus.FAILED, errorMessage(result));
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    if (result.wasRetried()) {
      return;
    }
    reportResult(result, TmsStatus.SKIPPED, errorMessage(result));
  }

  private void reportResult(ITestResult result, TmsStatus status, String comment) {
    system.ifPresent(tms -> {
      ITestNGMethod method = result.getMethod();
      boolean repeated = isRepeated(result);
      if (repeated && !runAvailable) {
        return;
      }
      Duration elapsed = Duration.ofMillis(Math.max(0, result.getEndMillis() - result.getStartMillis()));
      if (repeated) {
        InvocationCollector.Invocation invocation = new InvocationCollector.Invocation(
            status, comment, elapsed, InvocationCollector.render(result.getParameters()));
        TestUtils.getTmsLinks(method).forEach(caseId -> collector.add(collectedMethod(result), caseId, invocation));
        return;
      }
      TestUtils.getTmsLinks(method)
          .forEach(caseId -> report(tms, new TmsResult(caseId, status, comment, elapsed)));
    });
  }

  private void report(TestManagementSystem tms, TmsResult result) {
    if (!runAvailable) {
      return;
    }
    try {
      tms.report(result);
    } catch (RuntimeException e) {
      LOG.error("Can not report result of case '{}' to '{}': {}", result.caseId(), tms.id(), e.getMessage(), e);
    }
  }

  private synchronized Map<String, String> ticketComments() {
    if (Objects.isNull(ticketComments)) {
      try {
        ticketComments = TicketComments.byCaseId(tickets.get());
      } catch (RuntimeException e) {
        ticketComments = Map.of();
        LOG.error("Can not load tickets, excluded tests are reported only when disabled: {}", e.getMessage(), e);
      }
    }
    return ticketComments;
  }

  private void reportCollected(TestManagementSystem tms, Supplier<List<TmsResult>> drain) {
    try {
      drain.get().forEach(result -> report(tms, result));
    } catch (RuntimeException e) {
      LOG.error("Can not report repeated invocations to '{}': {}", tms.id(), e.getMessage(), e);
    }
  }

  /**
   * TestNG runs pooled invocations on clones of the method with an invocation count of 1, so the repeat is read
   * from the original method of the test context.
   */
  private static boolean isRepeated(ITestResult result) {
    ITestNGMethod method = result.getMethod();
    return method.isDataDriven() || method.getInvocationCount() > 1 || collectedMethod(result).getInvocationCount() > 1;
  }

  private static ITestNGMethod collectedMethod(ITestResult result) {
    ITestNGMethod method = result.getMethod();
    ITestContext context = result.getTestContext();
    if (Objects.isNull(context)) {
      return method;
    }
    return Arrays.stream(context.getAllTestMethods()).filter(method::equals).findFirst().orElse(method);
  }

  private static boolean shouldRun(TestManagementSystem tms, List<String> caseIds) {
    return caseIds.isEmpty() || caseIds.stream().anyMatch(tms::shouldRun);
  }

  private static String errorMessage(ITestResult result) {
    Throwable throwable = result.getThrowable();
    return Objects.nonNull(throwable) && Objects.nonNull(throwable.getMessage())
        ? throwable.getMessage()
        : NO_ERROR_MESSAGE;
  }
}
