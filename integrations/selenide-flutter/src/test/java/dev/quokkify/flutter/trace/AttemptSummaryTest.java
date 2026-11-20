package dev.quokkify.flutter.trace;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import dev.quokkify.flutter.FlutterBy;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AttemptSummaryTest {

  private static FindAttempt attempt(long ms, Outcome outcome) {
    return attempt(ms, outcome, null);
  }

  private static FindAttempt attempt(long ms, Outcome outcome, String detail) {
    return new FindAttempt(FlutterBy.key("delayed_result"), "flutter key \"delayed_result\"", Instant.EPOCH,
        Duration.ofMillis(ms), outcome, detail);
  }

  @Test
  public void summaryCollapsesRepeats() {
    AttemptSummary s = AttemptSummary.of(List.of(
        attempt(40, Outcome.NOT_FOUND), attempt(60, Outcome.NOT_FOUND), attempt(30, Outcome.FOUND)));

    assertThat(s.lines()).containsExactly(
        "2× flutter key \"delayed_result\" → NOT_FOUND (avg 50 ms)",
        "1× flutter key \"delayed_result\" → FOUND (avg 30 ms)");
    assertThat(s.count()).isEqualTo(3);
    assertThat(s.first()).isEqualTo(Duration.ofMillis(40));
    assertThat(s.last()).isEqualTo(Duration.ofMillis(30));
    assertThat(s.total()).isEqualTo(Duration.ofMillis(130));
    assertThat(s.finalOutcome()).isEqualTo(Outcome.FOUND);
    assertThat(s.render()).startsWith("3 find attempt(s), total 130 ms, result FOUND");
  }

  @Test
  public void averageTruncatesToMilliseconds() {
    AttemptSummary s = AttemptSummary.of(List.of(attempt(10, Outcome.NOT_FOUND), attempt(15, Outcome.NOT_FOUND)));

    assertThat(s.lines()).containsExactly("2× flutter key \"delayed_result\" → NOT_FOUND (avg 12 ms)");
  }

  @Test
  public void emptyListHasNoAttempts() {
    AttemptSummary s = AttemptSummary.of(List.of());

    assertThat(s.count()).isZero();
    assertThat(s.lines()).isEmpty();
    assertThat(s.render()).startsWith("0 find attempt(s), total 0 ms");
  }

  @Test
  public void errorGroupAppendsLastDetail() {
    AttemptSummary s = AttemptSummary.of(List.of(
        attempt(10, Outcome.ERROR, "first failure"), attempt(20, Outcome.ERROR, "session gone"),
        attempt(5, Outcome.NOT_FOUND, "ignored")));

    assertThat(s.lines()).containsExactly(
        "2× flutter key \"delayed_result\" → ERROR (avg 15 ms) — session gone",
        "1× flutter key \"delayed_result\" → NOT_FOUND (avg 5 ms)");
  }

  @Test
  public void errorGroupWithoutDetailHasNoSuffix() {
    AttemptSummary s = AttemptSummary.of(List.of(attempt(10, Outcome.ERROR)));

    assertThat(s.lines()).containsExactly("1× flutter key \"delayed_result\" → ERROR (avg 10 ms)");
  }

  @Test
  public void linesAreUnmodifiable() {
    List<String> source = new ArrayList<>(List.of("line"));
    AttemptSummary direct = new AttemptSummary(1, Duration.ZERO, Duration.ZERO, Duration.ZERO, Outcome.FOUND, source);
    source.add("late");

    assertThat(direct.lines()).containsExactly("line");
    assertThatThrownBy(() -> direct.lines().add("x")).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> AttemptSummary.of(List.of(attempt(1, Outcome.FOUND))).lines().add("x"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> AttemptSummary.of(List.of()).lines().add("x"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
