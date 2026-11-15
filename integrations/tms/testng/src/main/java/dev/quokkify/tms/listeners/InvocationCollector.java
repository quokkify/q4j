package dev.quokkify.tms.listeners;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import dev.quokkify.tms.TmsResult;
import dev.quokkify.tms.TmsStatus;

import org.testng.ITestNGMethod;

/**
 * Collects invocations of repeated test methods (data provider rows, {@code invocationCount}) and aggregates them
 * into one {@link TmsResult} per test method and case id. The worst status wins: failed, then skipped, then passed.
 * All methods are thread-safe, parallel data provider rows add invocations concurrently.
 */
final class InvocationCollector {

  static final int MAX_PARAMETERS_LENGTH = 200;
  private static final String TRUNCATION_MARK = "…";
  private static final String LINE_BREAKS = "\\s*\\R\\s*";

  private final Map<Key, List<Invocation>> invocations = new LinkedHashMap<>();

  /**
   * @param status     status of the invocation
   * @param comment    error message, may be {@code null}
   * @param elapsed    execution time, may be {@code null}
   * @param parameters data provider parameters, may be {@code null} or empty
   */
  record Invocation(TmsStatus status, String comment, Duration elapsed, Object[] parameters) {
  }

  private record Key(ITestNGMethod method, String caseId) {
  }

  synchronized void add(ITestNGMethod method, String caseId, Invocation invocation) {
    invocations.computeIfAbsent(new Key(method, caseId), key -> new ArrayList<>()).add(invocation);
  }

  /**
   * Removes and aggregates everything collected for the given methods.
   */
  synchronized List<TmsResult> drain(Collection<ITestNGMethod> methods) {
    Set<ITestNGMethod> drained = Set.copyOf(methods);
    return drainMatching(key -> drained.contains(key.method()));
  }

  /**
   * Removes and aggregates everything collected.
   */
  synchronized List<TmsResult> drainAll() {
    return drainMatching(key -> true);
  }

  private List<TmsResult> drainMatching(Predicate<Key> filter) {
    List<TmsResult> results = new ArrayList<>();
    invocations.entrySet().removeIf(entry -> {
      if (!filter.test(entry.getKey())) {
        return false;
      }
      results.add(aggregate(entry.getKey().caseId(), entry.getValue()));
      return true;
    });
    return results;
  }

  private static TmsResult aggregate(String caseId, List<Invocation> collected) {
    long failed = count(collected, TmsStatus.FAILED);
    long skipped = count(collected, TmsStatus.SKIPPED);
    long passed = count(collected, TmsStatus.PASSED);
    TmsStatus status = failed > 0 ? TmsStatus.FAILED : skipped > 0 ? TmsStatus.SKIPPED : TmsStatus.PASSED;
    return new TmsResult(caseId, status, comment(collected, failed, skipped, passed), elapsed(collected));
  }

  private static long count(List<Invocation> collected, TmsStatus status) {
    return collected.stream().filter(invocation -> invocation.status() == status).count();
  }

  private static String comment(List<Invocation> collected, long failed, long skipped, long passed) {
    List<String> counts = new ArrayList<>();
    if (failed > 0) {
      counts.add(failed + " failed");
    }
    if (skipped > 0) {
      counts.add(skipped + " skipped");
    }
    if (passed > 0) {
      counts.add(passed + " passed");
    }
    StringBuilder comment =
        new StringBuilder("%d invocations: %s".formatted(collected.size(), String.join(", ", counts)));
    for (int i = 0; i < collected.size(); i++) {
      Invocation invocation = collected.get(i);
      if (invocation.status() != TmsStatus.PASSED) {
        comment.append('\n').append(detail(i + 1, invocation));
      }
    }
    return comment.toString();
  }

  private static String detail(int number, Invocation invocation) {
    StringBuilder line = new StringBuilder("[%d] %s".formatted(number, invocation.status()));
    String parameters = parameters(invocation.parameters());
    if (!parameters.isEmpty()) {
      line.append(" (").append(parameters).append(')');
    }
    if (Objects.nonNull(invocation.comment())) {
      line.append(": ").append(invocation.comment().strip().replaceAll(LINE_BREAKS, " "));
    }
    return line.toString();
  }

  private static String parameters(Object[] parameters) {
    if (Objects.isNull(parameters) || parameters.length == 0) {
      return "";
    }
    String joined = Arrays.stream(parameters).map(String::valueOf).collect(Collectors.joining(", "));
    return joined.length() > MAX_PARAMETERS_LENGTH
        ? joined.substring(0, MAX_PARAMETERS_LENGTH - TRUNCATION_MARK.length()) + TRUNCATION_MARK
        : joined;
  }

  private static Duration elapsed(List<Invocation> collected) {
    return collected.stream()
        .map(Invocation::elapsed)
        .filter(Objects::nonNull)
        .reduce(Duration::plus)
        .orElse(null);
  }
}
