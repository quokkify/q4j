package dev.quokkify.flutter.comparison;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Renders the comparison of the modes a run used and decides whether it is acceptable.
 *
 * <p>Modes appear in the order they first ran. A mode named {@code <family>-<variant>} (such as {@code flutter-sync})
 * shares the known-gap clauses of its family ({@code flutter}).
 */
public final class ComparisonReport {

  public static final String OK = "OK";
  private static final Pattern CLAUSE = Pattern.compile("(?:known gap )?in (flutter|native) mode: ");
  private static final String MISSING = "- | - | -";

  private ComparisonReport() {
  }

  public static String render(List<ScenarioResult> results) {
    List<String> modes = modes(results);
    StringBuilder md = new StringBuilder("# Flutter vs native comparison\n\n| Scenario |");
    modes.forEach(mode -> md.append(' ').append(mode).append(" | ms | finds |"));
    md.append("\n| --- |").append(" --- | --- | --- |".repeat(modes.size())).append('\n');
    Map<String, List<ScenarioResult>> byScenario = results.stream()
        .collect(Collectors.groupingBy(ScenarioResult::scenario, TreeMap::new, Collectors.toList()));
    byScenario.forEach((scenario, rows) -> {
      md.append("| ").append(cellText(scenario)).append(" |");
      modes.forEach(mode -> md.append(' ').append(cells(rows, mode)).append(" |"));
      md.append('\n');
    });
    appendTiming(md, modes, byScenario);
    appendKnownGaps(md, results);
    appendDemo(md, modes, results);
    return md.toString();
  }

  private static List<String> modes(List<ScenarioResult> results) {
    return List.copyOf(results.stream().map(ScenarioResult::mode).collect(Collectors.toCollection(LinkedHashSet::new)));
  }

  static String family(String mode) {
    int dash = mode.indexOf('-');
    return dash < 0 ? mode : mode.substring(0, dash);
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
        if (result.diagnosticsExcerpt() == null) {
          return Optional.of("failure demo " + result.scenario() + " in " + result.mode()
              + " mode captured no diagnostics");
        }
      } else if (result.status() == Status.FAILED) {
        return Optional.of(result.scenario() + " failed in " + result.mode() + " mode");
      }
    }
    Set<String> modes = results.stream().map(ScenarioResult::mode).collect(Collectors.toCollection(TreeSet::new));
    Map<String, Set<String>> demoModes = results.stream().filter(ScenarioResult::expectedToFail)
        .collect(Collectors.groupingBy(ScenarioResult::scenario, TreeMap::new,
            Collectors.mapping(ScenarioResult::mode, Collectors.toSet())));
    for (Map.Entry<String, Set<String>> demo : demoModes.entrySet()) {
      for (String mode : modes) {
        if (!demo.getValue().contains(mode)) {
          return Optional.of("failure demo " + demo.getKey() + " has no result in " + mode + " mode");
        }
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

  private static String cellText(String text) {
    return text.replace("|", "\\|").replaceAll("\\R", " ");
  }

  /**
   * Keeps only the clause for the row's mode from a description like {@code base (known gap in flutter mode: a;
   * in native mode: b)}; any other description is returned whole.
   */
  static String reasonFor(ScenarioResult row) {
    String reason = row.knownGapReason();
    Matcher clause = CLAUSE.matcher(reason);
    int start = -1;
    int end = -1;
    String base = reason;
    while (clause.find()) {
      if (start < 0) {
        base = reason.substring(0, reason.lastIndexOf('(', clause.start())).trim();
      }
      if (clause.group(1).equals(family(row.mode()))) {
        start = clause.end();
        end = clause.find() ? clause.start() : reason.length();
        break;
      }
      start = clause.end();
    }
    if (end < 0) {
      return reason;
    }
    String text = reason.substring(start, end).replaceAll("[;)\\s]+$", "");
    return base.isEmpty() ? text : base + " (" + text + ")";
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
      gaps.forEach(gap -> md.append("| ").append(gap.mode()).append(" | ").append(cellText(gap.scenario()))
          .append(" | ").append(cellText(reasonFor(gap))).append(" |\n"));
    }
    notReproduced.forEach(r -> md.append("\n- known gap did not reproduce: ").append(r.mode()).append(" mode, ")
        .append(r.scenario()).append(" (").append(reasonFor(r)).append(")\n"));
  }

  /**
   * Per mode, the time and find attempts of the scenarios that passed in every mode, so the modes are compared on the
   * same work. ms per find divides scenario time (taps, typing and screen changes included) by find attempts.
   */
  private static void appendTiming(StringBuilder md, List<String> modes,
                                   Map<String, List<ScenarioResult>> byScenario) {
    List<List<ScenarioResult>> common = byScenario.values().stream()
        .filter(rows -> modes.stream().allMatch(mode -> rows.stream()
            .anyMatch(row -> mode.equals(row.mode()) && row.status() == Status.PASSED)))
        .toList();
    md.append("\n## Timing\n\n");
    if (common.isEmpty()) {
      md.append("No scenario passed in every mode.\n");
      return;
    }
    md.append("Scenarios that passed in every mode: ").append(common.size())
        .append(". ms per find = scenario ms / finds (scenario time includes taps, typing and screen changes).\n\n")
        .append("| Mode | ms | finds | ms per find |\n| --- | --- | --- | --- |\n");
    for (String mode : modes) {
      List<ScenarioResult> rows = common.stream().flatMap(List::stream).filter(row -> mode.equals(row.mode()))
          .toList();
      long ms = rows.stream().mapToLong(ScenarioResult::durationMs).sum();
      long finds = rows.stream().mapToLong(ScenarioResult::findAttempts).sum();
      md.append("| ").append(mode).append(" | ").append(ms).append(" | ").append(finds).append(" | ")
          .append(finds == 0 ? "-" : String.valueOf(Math.round((double) ms / finds))).append(" |\n");
    }
  }

  private static void appendDemo(StringBuilder md, List<String> modes, List<ScenarioResult> results) {
    Map<String, List<ScenarioResult>> demos = results.stream().filter(ScenarioResult::expectedToFail)
        .collect(Collectors.groupingBy(ScenarioResult::scenario, TreeMap::new, Collectors.toList()));
    if (demos.isEmpty()) {
      return;
    }
    md.append("\n## Failure demo\n");
    demos.forEach((scenario, rows) -> {
      md.append("\n### ").append(scenario).append("\n");
      for (String mode : modes) {
        rows.stream().filter(row -> mode.equals(row.mode())).findFirst().ifPresent(row -> md.append("\n**")
            .append(mode).append("** (").append(row.status()).append(")\n\n```\n")
            .append(row.diagnosticsExcerpt() == null ? "(no diagnostics captured)" : row.diagnosticsExcerpt())
            .append("\n```\n"));
      }
    });
  }
}
