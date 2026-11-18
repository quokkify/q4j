package dev.quokkify.elements.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Describes how an actual table value differs from the expected one in a few lines, so a failure on a table with
 * hundreds of rows does not print every row.
 */
final class TableDiff {

  static final int LIMIT = 10;

  private TableDiff() {
  }

  static String describe(Object expected, Object actual) {
    if (expected instanceof Map<?, ?> want && actual instanceof Map<?, ?> got) {
      return maps(want, got);
    }
    if (expected instanceof List<?> want && actual instanceof List<?> got) {
      return lists(want, got);
    }
    return String.valueOf(actual);
  }

  private static String maps(Map<?, ?> expected, Map<?, ?> actual) {
    List<String> missing = new ArrayList<>();
    List<String> different = new ArrayList<>();
    for (Map.Entry<?, ?> entry : expected.entrySet()) {
      if (!actual.containsKey(entry.getKey())) {
        missing.add(String.valueOf(entry.getKey()));
      } else if (!Objects.equals(entry.getValue(), actual.get(entry.getKey()))) {
        different.add(entry.getKey() + ": expected " + entry.getValue() + " but was " + actual.get(entry.getKey()));
      }
    }
    List<String> unexpected = actual.keySet().stream().filter(key -> !expected.containsKey(key))
        .map(String::valueOf).toList();
    List<String> lines = new ArrayList<>();
    lines.add(actual.size() + " rows, expected " + expected.size());
    section(lines, "missing", missing);
    section(lines, "unexpected", unexpected);
    section(lines, "different", different);
    return String.join("\n", lines);
  }

  private static String lists(List<?> expected, List<?> actual) {
    List<String> different = new ArrayList<>();
    for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
      if (!Objects.equals(expected.get(i), actual.get(i))) {
        different.add("row " + i + ": expected " + expected.get(i) + " but was " + actual.get(i));
      }
    }
    List<String> lines = new ArrayList<>();
    lines.add(actual.size() + " rows, expected " + expected.size());
    section(lines, "missing", indexed(expected, actual.size()));
    section(lines, "unexpected", indexed(actual, expected.size()));
    section(lines, "different", different);
    return String.join("\n", lines);
  }

  private static List<String> indexed(List<?> rows, int from) {
    List<String> result = new ArrayList<>();
    for (int i = from; i < rows.size(); i++) {
      result.add("row " + i + ": " + rows.get(i));
    }
    return result;
  }

  private static void section(List<String> lines, String name, List<String> entries) {
    if (entries.isEmpty()) {
      return;
    }
    lines.add(name + " (" + entries.size() + "):");
    entries.stream().limit(LIMIT).map(entry -> "  " + entry).forEach(lines::add);
    if (entries.size() > LIMIT) {
      lines.add("  ... " + (entries.size() - LIMIT) + " more");
    }
  }
}
