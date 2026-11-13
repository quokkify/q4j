package dev.quokkify.tms.listeners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.tms.TestManagementSystem;
import dev.quokkify.tms.TmsResult;
import dev.quokkify.tms.TmsStatus;
import dev.quokkify.tms.tickets.TicketComments;
import dev.quokkify.tms.tickets.TicketSource;

import io.qameta.allure.TmsLink;
import org.testng.IMethodInstance;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.internal.ConstructorOrMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TmsListenerTest {

  private RecordingSystem system;
  private List<TicketSource> tickets;
  private TmsListener listener;

  @BeforeMethod
  public void setUp() {
    system = new RecordingSystem();
    tickets = new ArrayList<>();
    listener = new TmsListener(() -> Optional.of(system), () -> tickets);
    listener.onExecutionStart();
  }

  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    listener.onExecutionFinish();
  }

  @TmsLink("TMS_LISTENER_1")
  @SingleThread
  @Test(description = "A passed test is reported as PASSED with its execution time")
  public void reportsPassedTest() throws NoSuchMethodException {
    listener.onTestSuccess(result("linkedToTwoCases", 1_000, 3_500, null));

    assertThat(system.reported).extracting(TmsResult::caseId).containsExactly("1", "2");
    assertThat(system.reported).allSatisfy(result -> {
      assertThat(result.status()).isEqualTo(TmsStatus.PASSED);
      assertThat(result.elapsed()).hasMillis(2_500);
      assertThat(result.comment()).isNull();
    });
  }

  @TmsLink("TMS_LISTENER_2")
  @SingleThread
  @Test(description = "A failed test is reported as FAILED with the error message")
  public void reportsFailedTestWithMessage() throws NoSuchMethodException {
    listener.onTestFailure(result("linkedToOneCase", 0, 10, new AssertionError("expected 1 but was 2")));

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.status()).isEqualTo(TmsStatus.FAILED);
      assertThat(result.comment()).isEqualTo("expected 1 but was 2");
    });
  }

  @TmsLink("TMS_LISTENER_3")
  @SingleThread
  @Test(description = "A skipped test is reported as SKIPPED, a retried attempt is not reported")
  public void reportsSkippedButNotRetriedTest() throws NoSuchMethodException {
    ITestResult retried = result("linkedToOneCase", 0, 10, new IllegalStateException("flaky"));
    when(retried.wasRetried()).thenReturn(true);
    listener.onTestSkipped(retried);
    listener.onTestSkipped(result("linkedToOneCase", 0, 10, null));

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.status()).isEqualTo(TmsStatus.SKIPPED);
      assertThat(result.comment()).isEqualTo("Error message not found");
    });
  }

  @TmsLink("TMS_LISTENER_4")
  @SingleThread
  @Test(description = "A test without case ids is not reported")
  public void ignoresTestWithoutCaseIds() throws NoSuchMethodException {
    listener.onTestSuccess(result("notLinked", 0, 10, null));

    assertThat(system.reported).isEmpty();
  }

  @TmsLink("TMS_LISTENER_5")
  @SingleThread
  @Test(description = "Suite start passes case ids of all suite methods to the system once each")
  public void startsRunWithSuiteCaseIds() throws NoSuchMethodException {
    listener.startRun(List.of(
        testMethod("linkedToTwoCases"), testMethod("linkedToOneCase"), testMethod("notLinked")));

    assertThat(system.startedWith).containsExactly(Set.of("1", "2", "3"));
  }

  @TmsLink("TMS_LISTENER_6")
  @SingleThread
  @Test(description = "When the run can not be started, results are not reported and tests are not failed")
  public void skipsReportingWhenRunStartFails() throws NoSuchMethodException {
    system.failOnStart = true;
    listener.startRun(List.of(testMethod("linkedToOneCase")));
    listener.onTestSuccess(result("linkedToOneCase", 0, 10, null));

    assertThat(system.reported).isEmpty();
  }

  @TmsLink("TMS_LISTENER_7")
  @SingleThread
  @Test(description = "A reporting error does not propagate to the test")
  public void swallowsReportingErrors() throws NoSuchMethodException {
    system.failOnReport = true;

    listener.onTestSuccess(result("linkedToOneCase", 0, 10, null));

    assertThat(system.reportAttempts).isEqualTo(1);
  }

  @TmsLink("TMS_LISTENER_8")
  @SingleThread
  @Test(description = "Tests disabled via 'enabled' are reported as BLOCKED when the suite finishes")
  public void reportsDisabledTestsAsBlocked() throws NoSuchMethodException {
    ITestNGMethod disabled = testMethod("linkedToOneCase");
    when(disabled.getEnabled()).thenReturn(false);
    ITestNGMethod excludedByGroup = testMethod("linkedToTwoCases");
    when(excludedByGroup.getEnabled()).thenReturn(true);
    listener.reportDisabled(List.of(disabled, excludedByGroup));

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.caseId()).isEqualTo("3");
      assertThat(result.status()).isEqualTo(TmsStatus.BLOCKED);
      assertThat(result.comment()).isEqualTo("3 is disabled via 'enabled' attribute.");
    });
  }

  @TmsLink("TMS_LISTENER_9")
  @SingleThread
  @Test(description = "The run is finished once after all suites")
  public void finishesRunAfterExecution() {
    listener.onExecutionFinish();

    assertThat(system.finished).isEqualTo(1);
  }

  @TmsLink("TMS_LISTENER_10")
  @SingleThread
  @Test(description = "Nothing is reported when no system is selected")
  public void doesNothingWithoutSelectedSystem() throws NoSuchMethodException {
    TmsListener disabledListener = new TmsListener(Optional::empty, List::of);
    disabledListener.onExecutionStart();

    disabledListener.onTestSuccess(result("linkedToOneCase", 0, 10, null));
    disabledListener.onExecutionFinish();

    assertThat(system.reported).isEmpty();
    assertThat(system.finished).isZero();
  }

  @TmsLink("TMS_LISTENER_12")
  @SingleThread
  @Test(description = "Reporting resumes when a later suite starts the run successfully")
  public void resumesAfterSuccessfulStart() throws NoSuchMethodException {
    system.failOnStart = true;
    listener.startRun(List.of(testMethod("linkedToOneCase")));
    system.failOnStart = false;
    listener.startRun(List.of(testMethod("linkedToOneCase")));

    listener.onTestSuccess(result("linkedToOneCase", 0, 10, null));

    assertThat(system.reported).hasSize(1);
  }

  @TmsLink("TMS_LISTENER_13")
  @SingleThread
  @Test(description = "A failure within successPercentage is reported as PASSED")
  public void reportsFailureWithinSuccessPercentageAsPassed() throws NoSuchMethodException {
    listener.onTestFailedButWithinSuccessPercentage(result("linkedToOneCase", 0, 10, new AssertionError("1 of 10")));

    assertThat(system.reported).singleElement().extracting(TmsResult::status).isEqualTo(TmsStatus.PASSED);
  }

  @TmsLink("TMS_LISTENER_14")
  @SingleThread
  @Test(description = "An excluded enabled test with a ticket is reported as BLOCKED with the ticket comment")
  public void excludedEnabledTestWithTicketIsBlocked() throws NoSuchMethodException {
    TicketSource jira = new StubTicketSource(Map.of("6", List.of("J-1")));
    tickets.add(jira);
    ITestNGMethod excluded = testMethod(ClassLinkedTests.class, "inheritsClassLink");
    when(excluded.getEnabled()).thenReturn(true);

    listener.reportDisabled(List.of(excluded));

    assertThat(system.reported).singleElement().satisfies(result -> {
      assertThat(result.caseId()).isEqualTo("6");
      assertThat(result.status()).isEqualTo(TmsStatus.BLOCKED);
      assertThat(result.comment()).isEqualTo(TicketComments.byCaseId(List.of(jira)).get("6"));
    });
  }

  @TmsLink("TMS_LISTENER_15")
  @SingleThread
  @Test(description = "The interceptor drops tests whose case ids the system rejects and keeps unlinked tests")
  public void interceptorDropsTestsTheSystemRejects() throws NoSuchMethodException {
    system.rejected.add("3");
    IMethodInstance twoCases = methodInstance("linkedToTwoCases");
    IMethodInstance oneCase = methodInstance("linkedToOneCase");
    IMethodInstance notLinked = methodInstance("notLinked");

    List<IMethodInstance> kept = listener.intercept(List.of(twoCases, oneCase, notLinked), null);

    assertThat(kept).containsExactly(twoCases, notLinked);
  }

  @TmsLink("TMS_LISTENER_16")
  @SingleThread
  @Test(description = "The listener is active only between execution start and finish")
  public void activeOnlyDuringExecution() {
    listener.onExecutionFinish();
    TmsListener another = new TmsListener(Optional::empty, List::of);
    assertThat(TmsListener.isActive()).as("Active before execution start").isFalse();

    another.onExecutionStart();
    assertThat(TmsListener.isActive()).as("Active during execution").isTrue();

    another.onExecutionFinish();
    assertThat(TmsListener.isActive()).as("Active after execution finish").isFalse();
  }

  private static ITestResult result(String methodName, long start, long end, Throwable throwable)
      throws NoSuchMethodException {
    ITestNGMethod method = testMethod(methodName);
    ITestResult result = mock(ITestResult.class);
    when(result.getMethod()).thenReturn(method);
    when(result.getStartMillis()).thenReturn(start);
    when(result.getEndMillis()).thenReturn(end);
    when(result.getThrowable()).thenReturn(throwable);
    return result;
  }

  private static IMethodInstance methodInstance(String methodName) throws NoSuchMethodException {
    ITestNGMethod method = testMethod(methodName);
    IMethodInstance instance = mock(IMethodInstance.class);
    when(instance.getMethod()).thenReturn(method);
    return instance;
  }

  private static ITestNGMethod testMethod(String methodName) throws NoSuchMethodException {
    return testMethod(SampleTests.class, methodName);
  }

  private static ITestNGMethod testMethod(Class<?> testClass, String methodName) throws NoSuchMethodException {
    ITestNGMethod method = mock(ITestNGMethod.class);
    when(method.getConstructorOrMethod())
        .thenReturn(new ConstructorOrMethod(testClass.getDeclaredMethod(methodName)));
    return method;
  }

  @SuppressWarnings("unused")
  private static final class SampleTests {

    @TmsLink("1")
    @TmsLink("2")
    void linkedToTwoCases() {
    }

    @TmsLink("3")
    void linkedToOneCase() {
    }

    void notLinked() {
    }
  }

  @SuppressWarnings("unused")
  @TmsLink("6")
  private static final class ClassLinkedTests {

    void inheritsClassLink() {
    }
  }

  private record StubTicketSource(Map<String, List<String>> getTestCasesWithBugs) implements TicketSource {

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    public String buildTicketLink(String ticketId) {
      return "[%s](https://jira.example/%s)".formatted(ticketId, ticketId);
    }

    @Override
    public String label() {
      return "Jira Issue(s)";
    }
  }

  private static final class RecordingSystem implements TestManagementSystem {

    private final List<TmsResult> reported = new ArrayList<>();
    private final List<Set<String>> startedWith = new ArrayList<>();
    private final Set<String> rejected = new HashSet<>();
    private boolean failOnStart;
    private boolean failOnReport;
    private int reportAttempts;
    private int finished;

    @Override
    public String id() {
      return "recording";
    }

    @Override
    public boolean isConfigured() {
      return true;
    }

    @Override
    public boolean shouldRun(String caseId) {
      return !rejected.contains(caseId);
    }

    @Override
    public void startRun(Set<String> caseIds) {
      if (failOnStart) {
        throw new IllegalStateException("401 Unauthorized");
      }
      startedWith.add(caseIds);
    }

    @Override
    public void report(TmsResult result) {
      reportAttempts++;
      if (failOnReport) {
        throw new IllegalStateException("500 Internal Server Error");
      }
      reported.add(result);
    }

    @Override
    public void finishRun() {
      finished++;
    }
  }
}
