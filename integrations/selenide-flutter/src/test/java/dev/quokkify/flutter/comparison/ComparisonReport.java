package dev.quokkify.flutter.comparison;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Renders the flutter-versus-native comparison and decides whether it is acceptable.
 */
public final class ComparisonReport {

  public static final String OK = "OK";
  private static final String FLUTTER = "flutter";
  private static final String NATIVE = "native";
  private static final String MISSING = "- | - | -";

  private ComparisonReport() {
  }

  public static String render(List<ScenarioResult> results) {
    StringBuilder md = new StringBuilder("# Flutter vs native comparison\n\n");
    md.append("| Scenario | Flutter | ms | finds | Native | ms | finds |\n");
    md.append("| --- | --- | --- | --- | --- | --- | --- |\n");
    Map<String, List<ScenarioResult>> byScenario = results.stream()
        .collect(Collectors.groupingBy(ScenarioResult::scenario, TreeMap::new, Collectors.toList()));
    byScenario.forEach((scenario, rows) -> md.append("| ").append(scenario).append(" | ")
        .append(cells(rows, FLUTTER)).append(" | ").append(cells(rows, NATIVE)).append(" |\n"));
    appendKnownGaps(md, results);
    appendDemo(md, results);
    return md.toString();
  }

  public static Optional<String> verdict(List<ScenarioResult> results) {
    if (results.isEmpty()) {
      return Optional.of("no results were recorded");
    }
    for (ScenarioResult result : results) {
      if (result.expectedToFail()) {
        if (result.status() != Status.FAILED) {
          return Optional.of("failure demo " + describe(result.status()) + " in " + result.mode() + " mode ("
              + result.scenario() + ")");
        }
      } else if (result.status() == Status.FAILED) {
        return Optional.of(result.scenario() + " failed in " + result.mode() + " mode");
      }
    }
    return Optional.empty();
  }

  private static String describe(Status status) {
    return status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
  }

  private static String cells(List<ScenarioResult> rows, String mode) {
    return rows.stream().filter(row -> mode.equals(row.mode())).findFirst()
        .map(row -> cell(row) + " | " + row.durationMs() + " | " + row.findAttempts())
        .orElse(MISSING);
  }

  private static String cell(ScenarioResult row) {
    if (row.status() == Status.PASSED && row.knownGapReason() != null) {
      return "PASSED (known gap did not reproduce)";
    }
    return row.status().name();
  }

  private static void appendKnownGaps(StringBuilder md, List<ScenarioResult> results) {
    List<ScenarioResult> gaps = results.stream().filter(r -> r.status() == Status.KNOWN_GAP)
        .sorted(Comparator.comparing(ScenarioResult::mode).thenComparing(ScenarioResult::scenario)).toList();
    List<ScenarioResult> notReproduced = results.stream()
        .filter(r -> r.status() == Status.PASSED && r.knownGapReason() != null).toList();
    if (gaps.isEmpty() && notReproduced.isEmpty()) {
      return;
    }
    md.append("\n## Known gaps\n\n");
    if (!gaps.isEmpty()) {
      md.append("| Mode | Scenario | Reason |\n| --- | --- | --- |\n");
      gaps.forEach(gap -> md.append("| ").append(gap.mode()).append(" | ").append(gap.scenario()).append(" | ")
          .append(gap.knownGapReason()).append(" |\n"));
    }
    notReproduced.forEach(r -> md.append("\n- known gap did not reproduce: ").append(r.mode()).append(" mode, ")
        .append(r.scenario()).append(" (").append(r.knownGapReason()).append(")\n"));
  }

  private static void appendDemo(StringBuilder md, List<ScenarioResult> results) {
    Map<String, List<ScenarioResult>> demos = results.stream().filter(ScenarioResult::expectedToFail)
        .collect(Collectors.groupingBy(ScenarioResult::scenario, TreeMap::new, Collectors.toList()));
    if (demos.isEmpty()) {
      return;
    }
    md.append("\n## Failure demo\n");
    demos.forEach((scenario, rows) -> {
      md.append("\n### ").append(scenario).append("\n");
      for (String mode : List.of(FLUTTER, NATIVE)) {
        rows.stream().filter(row -> mode.equals(row.mode())).findFirst().ifPresent(row -> md.append("\n**")
            .append(mode).append("** (").append(row.status()).append(")\n\n```\n")
            .append(row.diagnosticsExcerpt() == null ? "(no diagnostics captured)" : row.diagnosticsExcerpt())
            .append("\n```\n"));
      }
    });
  }
}
