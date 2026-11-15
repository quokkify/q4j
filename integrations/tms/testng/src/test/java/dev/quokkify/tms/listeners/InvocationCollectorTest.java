package dev.quokkify.tms.listeners;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.tms.TmsResult;
import dev.quokkify.tms.TmsStatus;
import dev.quokkify.tms.listeners.InvocationCollector.Invocation;

import io.qameta.allure.TmsLink;
import org.testng.ITestNGMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

public class InvocationCollectorTest {

  private static final String CASE_ID = "1";

  private InvocationCollector collector;
  private ITestNGMethod method;

  @BeforeMethod
  public void setUp() {
    collector = new InvocationCollector();
    method = mock(ITestNGMethod.class);
  }

  @TmsLink("TMS_COLLECTOR_1")
  @SingleThread
  @Test(description = "A failure wins over a skip and a pass, and non-passed invocations are listed")
  public void failedWinsOverSkippedAndPassed() {
    collector.add(method, CASE_ID, invocation(TmsStatus.FAILED, "boom"));
    collector.add(method, CASE_ID, invocation(TmsStatus.SKIPPED, "skip"));
    collector.add(method, CASE_ID, invocation(TmsStatus.PASSED, null));

    assertThat(collector.drainAll()).singleElement().satisfies(result -> {
      assertThat(result.status()).isEqualTo(TmsStatus.FAILED);
      assertThat(result.comment().lines()).first().isEqualTo("3 invocations: 1 failed, 1 skipped, 1 passed");
      assertThat(result.comment()).contains("[1] FAILED", "[2] SKIPPED").doesNotContain("[3]");
    });
  }

  @TmsLink("TMS_COLLECTOR_2")
  @SingleThread
  @Test(description = "A skip wins over a pass")
  public void skippedWinsOverPassed() {
    collector.add(method, CASE_ID, invocation(TmsStatus.SKIPPED, "skip"));
    collector.add(method, CASE_ID, invocation(TmsStatus.PASSED, null));

    assertThat(collector.drainAll()).singleElement().extracting(TmsResult::status).isEqualTo(TmsStatus.SKIPPED);
  }

  @TmsLink("TMS_COLLECTOR_3")
  @SingleThread
  @Test(description = "All passed invocations give a PASSED result with the summary line only")
  public void allPassedGivesSummaryOnly() {
    for (int i = 0; i < 3; i++) {
      collector.add(method, CASE_ID, invocation(TmsStatus.PASSED, null));
    }

    assertThat(collector.drainAll()).singleElement().satisfies(result -> {
      assertThat(result.status()).isEqualTo(TmsStatus.PASSED);
      assertThat(result.comment()).isEqualTo("3 invocations: 3 passed");
    });
  }

  @TmsLink("TMS_COLLECTOR_4")
  @SingleThread
  @Test(description = "Elapsed time is the sum of the known durations, null when none is known")
  public void sumsElapsed() {
    collector.add(method, CASE_ID, new Invocation(TmsStatus.PASSED, null, Duration.ofSeconds(1), null));
    collector.add(method, CASE_ID, new Invocation(TmsStatus.PASSED, null, Duration.ofSeconds(2), null));
    collector.add(method, CASE_ID, new Invocation(TmsStatus.PASSED, null, null, null));
    collector.add(method, "2", new Invocation(TmsStatus.PASSED, null, null, null));

    assertThat(collector.drainAll()).extracting(TmsResult::elapsed)
        .containsExactly(Duration.ofSeconds(3), null);
  }

  @TmsLink("TMS_COLLECTOR_5")
  @SingleThread
  @Test(description = "An invocation without parameters is rendered without parentheses")
  public void rendersNoParametersWithoutParentheses() {
    collector.add(method, CASE_ID, new Invocation(TmsStatus.PASSED, null, null, new Object[0]));
    collector.add(method, CASE_ID, new Invocation(TmsStatus.FAILED, "boom", null, null));

    assertThat(collector.drainAll()).singleElement()
        .satisfies(result -> assertThat(result.comment().lines()).contains("[2] FAILED: boom"));
  }

  @TmsLink("TMS_COLLECTOR_6")
  @SingleThread
  @Test(description = "A long parameter list is truncated to the maximum length with an ellipsis")
  public void truncatesLongParameters() {
    collector.add(method, CASE_ID,
        new Invocation(TmsStatus.FAILED, "boom", null, new Object[] {null, "x".repeat(500)}));

    assertThat(collector.drainAll()).singleElement().satisfies(result -> {
      String line = result.comment().lines().skip(1).findFirst().orElseThrow();
      String parameters = line.substring(line.indexOf('(') + 1, line.lastIndexOf("): "));
      assertThat(parameters).startsWith("null, ").hasSize(InvocationCollector.MAX_PARAMETERS_LENGTH).endsWith("…");
    });
  }

  @TmsLink("TMS_COLLECTOR_7")
  @SingleThread
  @Test(description = "A multi-line message is collapsed into a single detail line")
  public void collapsesMultiLineMessage() {
    collector.add(method, CASE_ID, invocation(TmsStatus.FAILED, "Expecting:\n  <1>\r\nto be equal to:\n  <2>\n"));

    assertThat(collector.drainAll()).singleElement().satisfies(result -> {
      assertThat(result.comment().lines()).hasSize(2);
      assertThat(result.comment().lines()).contains("[1] FAILED: Expecting: <1> to be equal to: <2>");
    });
  }

  @TmsLink("TMS_COLLECTOR_8")
  @SingleThread
  @Test(description = "One method linked to two cases yields one result per case id")
  public void yieldsOneResultPerCaseId() {
    collector.add(method, "1", invocation(TmsStatus.PASSED, null));
    collector.add(method, "2", invocation(TmsStatus.FAILED, "boom"));

    assertThat(collector.drainAll()).extracting(TmsResult::caseId, TmsResult::status)
        .containsExactly(
            tuple("1", TmsStatus.PASSED),
            tuple("2", TmsStatus.FAILED));
  }

  @TmsLink("TMS_COLLECTOR_9")
  @SingleThread
  @Test(description = "Draining some methods leaves the entries of the others, and draining twice returns nothing")
  public void drainsOnlyRequestedMethods() {
    ITestNGMethod other = mock(ITestNGMethod.class);
    collector.add(method, "1", invocation(TmsStatus.PASSED, null));
    collector.add(other, "2", invocation(TmsStatus.PASSED, null));

    assertThat(collector.drain(List.of(method))).extracting(TmsResult::caseId).containsExactly("1");
    assertThat(collector.drainAll()).extracting(TmsResult::caseId).containsExactly("2");
    assertThat(collector.drainAll()).isEmpty();
  }

  @TmsLink("TMS_COLLECTOR_10")
  @SingleThread
  @Test(description = "Invocations added from several threads are all counted")
  public void countsConcurrentInvocations() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int i = 0; i < 50; i++) {
        futures.add(pool.submit(() -> collector.add(method, CASE_ID, invocation(TmsStatus.PASSED, null))));
      }
      for (Future<?> future : futures) {
        future.get();
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(collector.drainAll()).singleElement()
        .satisfies(result -> assertThat(result.comment().lines()).first().isEqualTo("50 invocations: 50 passed"));
  }

  private static Invocation invocation(TmsStatus status, String comment) {
    return new Invocation(status, comment, null, null);
  }
}
