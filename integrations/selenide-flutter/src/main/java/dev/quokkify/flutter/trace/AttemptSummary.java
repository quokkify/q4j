package dev.quokkify.flutter.trace;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public record AttemptSummary(int count, Duration first, Duration last, Duration total, Outcome finalOutcome,
                             List<String> lines) {

  public static AttemptSummary of(List<FindAttempt> attempts) {
    if (attempts.isEmpty()) {
      return new AttemptSummary(0, Duration.ZERO, Duration.ZERO, Duration.ZERO, null, List.of());
    }
    Duration total = Duration.ZERO;
    for (FindAttempt attempt : attempts) {
      total = total.plus(attempt.duration());
    }
    return new AttemptSummary(attempts.size(), attempts.get(0).duration(),
        attempts.get(attempts.size() - 1).duration(), total, attempts.get(attempts.size() - 1).outcome(),
        collapse(attempts));
  }

  public String render() {
    StringBuilder out = new StringBuilder()
        .append(count).append(" find attempt(s), total ").append(total.toMillis()).append(" ms");
    if (finalOutcome != null) {
      out.append(", result ").append(finalOutcome);
    }
    for (String line : lines) {
      out.append(System.lineSeparator()).append("  ").append(line);
    }
    return out.toString();
  }

  private static List<String> collapse(List<FindAttempt> attempts) {
    List<String> lines = new ArrayList<>();
    int i = 0;
    while (i < attempts.size()) {
      FindAttempt head = attempts.get(i);
      Duration sum = Duration.ZERO;
      int n = 0;
      while (i < attempts.size() && attempts.get(i).locator().equals(head.locator())
          && attempts.get(i).outcome() == head.outcome()) {
        sum = sum.plus(attempts.get(i).duration());
        n++;
        i++;
      }
      lines.add(n + "× " + head.locator() + " → " + head.outcome() + " (avg " + sum.dividedBy(n).toMillis() + " ms)");
    }
    return lines;
  }
}
