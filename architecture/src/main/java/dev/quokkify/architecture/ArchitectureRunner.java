package dev.quokkify.architecture;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.ArchitectureRule;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;
import dev.quokkify.architecture.exceptions.ArchitectureViolationException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Single entry point of the architecture verification, run as a main class by the consumer's build.
 *
 * <p>Every discovered rule is evaluated against one shared {@link ArchitectureContext}, which covers the
 * packages named by {@link ArchitectureContext#PACKAGES_PROPERTY}, and reported at its own severity.
 * Evaluation never stops early, so one run reports every finding.
 *
 * <p>Rules are discovered through the {@link ServiceLoader}, so this class has no compile time knowledge of
 * them and the module can be published as a library that a consumer extends with its own rules. They are
 * evaluated concurrently on virtual threads, and the report is ordered by rule name rather than by
 * completion, so the output depends neither on the classpath order nor on which rule finishes first.
 *
 * <p>Whether the build fails is a separate decision, taken once at the end by comparing the worst finding
 * against the gate threshold from {@link #FAIL_ON_PROPERTY}. By default both errors and warnings fail;
 * {@code -Darchitecture.fail.on=ERROR} lets warnings through and {@code NEVER} reports without failing.
 *
 * <p>To add a rule, implement {@link ArchitectureRule} and list it in
 * {@code META-INF/services/dev.quokkify.architecture.contract.ArchitectureRule}.
 * No build configuration change is required.
 */
public final class ArchitectureRunner {

  /**
   * Severity the gate fails on, or {@code NEVER} to report without failing. Least severe wins: at
   * {@code WARNING} both warnings and errors fail, at {@code ERROR} only errors do.
   */
  static final String FAIL_ON_PROPERTY = "architecture.fail.on";

  /**
   * Both errors and warnings fail the build unless the threshold is lowered. A finding that genuinely must
   * not block anyone belongs at {@link RuleSeverity#INFO}.
   */
  static final RuleSeverity DEFAULT_FAIL_ON = RuleSeverity.WARNING;

  private static final String NEVER = "NEVER";

  private static final Logger LOG = LogManager.getLogger(ArchitectureRunner.class);

  private static final String SEPARATOR = "=".repeat(60);
  private static final String CONTINUATION_INDENT = "    ";
  private static final String LINE_FORMAT = "%-7s %s";

  private ArchitectureRunner() {
  }

  /**
   * Runs every registered rule and fails the build when a finding reaches the gate threshold.
   *
   * <p>The threshold comes from {@link #FAIL_ON_PROPERTY} and defaults to {@link #DEFAULT_FAIL_ON}, so both
   * errors and warnings fail the build. Passing {@code -Darchitecture.fail.on=ERROR} lets warnings through,
   * {@code INFO} fails on anything, and {@code NEVER} turns the run into a report.
   *
   * @param args ignored
   * @throws ArchitectureViolationException when a finding reaches the threshold
   */
  public static void main(String[] args) {
    Optional<RuleSeverity> failOn = failOnThreshold();
    Report report;
    try (ArchitectureContext context = ArchitectureContext.fromSystemProperties()) {
      report = verify(discoverRules(), context);
    }
    boolean failing = failOn.isPresent() && report.hasFindingAtOrAbove(failOn.get());
    LOG.info("Gate: fail on {} -> {}",
        failOn.map(RuleSeverity::name).orElse(NEVER), failing ? "failed" : "passed");
    if (failing) {
      throw new ArchitectureViolationException(
          "Architecture verification failed: %d error(s), %d warning(s), %d info, gate fails on %s. "
              .formatted(report.errors(), report.warnings(), report.infos(), failOn.orElseThrow().name())
              + "See the report above.");
    }
  }

  /**
   * Reads the gate threshold from {@link #FAIL_ON_PROPERTY}.
   *
   * @return the severity to fail on, or empty when configured to never fail
   */
  static Optional<RuleSeverity> failOnThreshold() {
    return parseFailOn(System.getProperty(FAIL_ON_PROPERTY, DEFAULT_FAIL_ON.name()));
  }

  /**
   * Parses a gate threshold, kept separate from the system property so it can be tested without touching
   * global state, which tests running in parallel could not do safely.
   *
   * @param configured a severity name, or {@code NEVER}, case insensitive
   * @return the severity to fail on, or empty for {@code NEVER}
   */
  static Optional<RuleSeverity> parseFailOn(String configured) {
    String value = Objects.isNull(configured) ? "" : configured.trim();
    if (NEVER.equalsIgnoreCase(value)) {
      return Optional.empty();
    }
    try {
      return Optional.of(RuleSeverity.valueOf(value.toUpperCase(Locale.ENGLISH)));
    } catch (IllegalArgumentException unknown) {
      throw new ArchitectureRunnerError(
          "Unknown value '%s' for -D%s. Use INFO, WARNING, ERROR or NEVER.".formatted(value, FAIL_ON_PROPERTY),
          unknown);
    }
  }

  /**
   * Discovers every rule published on the classpath through the {@link ServiceLoader}.
   *
   * <p>A rule is registered by naming its class in
   * {@code META-INF/services/dev.quokkify.architecture.contract.ArchitectureRule}, which is
   * what lets a consumer of this module contribute rules from its own jar without this class knowing them.
   * Every provider therefore needs a public no-argument constructor.
   *
   * <p>The result is sorted by {@link ArchitectureRule#name()} so that the report reads the same whatever
   * order the classpath yields the provider files in.
   *
   * @return every rule verified during the build, never empty
   * @throws ArchitectureRunnerError when no rule is published, or a provider cannot be instantiated
   */
  public static List<ArchitectureRule> discoverRules() {
    List<ArchitectureRule> rules;
    try {
      rules = ServiceLoader.load(ArchitectureRule.class).stream()
          .map(ServiceLoader.Provider::get)
          .sorted(Comparator.comparing(ArchitectureRule::name))
          .toList();
    } catch (ServiceConfigurationError broken) {
      throw new ArchitectureRunnerError("""
          An ArchitectureRule provider could not be loaded, so the verification did not run. Every class \
          listed in META-INF/services/dev.quokkify.architecture.contract.ArchitectureRule \
          must exist, implement ArchitectureRule and declare a public no-argument constructor.""", broken);
    }
    if (rules.isEmpty()) {
      // An empty rule set would report "0 passed" and leave the build green, which is the one failure this
      // gate must never have: it would silently stop verifying anything.
      throw new ArchitectureRunnerError("""
          No ArchitectureRule was discovered on the classpath, so no contract was verified at all. Rules are \
          registered in META-INF/services/dev.quokkify.architecture.contract.ArchitectureRule; \
          check that the file is present in the built artefact and names at least one rule.""");
    }
    return rules;
  }

  /**
   * Evaluates every rule against the given context and prints one report line group per rule.
   *
   * <p>Rules run concurrently, one virtual thread per rule, because they are independent: each one only
   * reads the shared context and reports through its return or its throwable. The two classpath scans
   * behind {@link ArchitectureContext} therefore overlap instead of running one after the other.
   *
   * <p>Each rule is isolated: a throwable raised by one rule is recorded against its own severity and never
   * prevents the remaining rules from running. The report is emitted in the order of {@code rules}, not in
   * completion order, so a concurrent run and a sequential one produce identical output.
   *
   * @param rules   rules to evaluate
   * @param context shared classpath model
   * @return aggregated outcome of the run
   */
  public static Report verify(List<ArchitectureRule> rules, ArchitectureContext context) {
    Map<RuleSeverity, Integer> violated = new EnumMap<>(RuleSeverity.class);
    for (RuleSeverity severity : RuleSeverity.values()) {
      violated.put(severity, 0);
    }

    List<String> lines = new ArrayList<>();
    lines.add(SEPARATOR);
    lines.add("Architecture verification (%d rules)".formatted(rules.size()));
    lines.add(SEPARATOR);

    long startedAt = System.nanoTime();
    List<RuleOutcome> outcomes = evaluateAll(rules, context);
    long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

    RuleSeverity worst = null;
    int evaluated = 0;
    Error unrecoverable = null;
    // The report is emitted in a finally block so that an ArchitectureRunnerError, which aborts the run,
    // still leaves behind the findings of the rules that did complete.
    try {
      for (RuleOutcome outcome : outcomes) {
        if (Objects.nonNull(outcome.unrecoverable())) {
          unrecoverable = keepFirst(unrecoverable, outcome.unrecoverable());
          continue;
        }
        evaluated++;
        ArchitectureRule rule = outcome.rule();
        if (Objects.isNull(outcome.finding())) {
          lines.add(LINE_FORMAT.formatted("[PASS]", rule.name()));
          continue;
        }
        RuleSeverity severity = rule.severity();
        lines.add(LINE_FORMAT.formatted("[%s]".formatted(severity.getLabel()), rule.name()));
        lines.add(indent(describe(outcome.finding())));
        violated.merge(severity, 1, Integer::sum);
        if (Objects.isNull(worst) || severity.compareTo(worst) > 0) {
          worst = severity;
        }
      }
    } finally {
      emit(lines, toReport(violated), evaluated, rules.size(), worst, elapsedMillis);
    }
    if (Objects.nonNull(unrecoverable)) {
      throw unrecoverable;
    }
    return toReport(violated);
  }

  /**
   * Submits every rule to a virtual thread executor and collects the outcomes in the order of {@code rules}.
   *
   * <p>The executor is closed by the try-with-resources block, which waits for every task, so no rule can
   * still be reading the context once the caller closes it.
   */
  private static List<RuleOutcome> evaluateAll(List<ArchitectureRule> rules, ArchitectureContext context) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<Throwable>> pending = rules.stream()
          .map(rule -> executor.submit(() -> evaluate(rule, context)))
          .toList();
      List<RuleOutcome> outcomes = new ArrayList<>(rules.size());
      for (int index = 0; index < rules.size(); index++) {
        outcomes.add(outcomeOf(rules.get(index), pending.get(index)));
      }
      return outcomes;
    }
  }

  /**
   * {@link #evaluate} already converts an {@link AssertionError} and every {@link Exception} into a finding,
   * so anything surfacing here as a failure is an {@link Error} that must abort the run instead of being
   * reported at the rule's severity. It is carried out of the task rather than thrown at once, so the
   * findings of the rules that did complete are still reported.
   */
  private static RuleOutcome outcomeOf(ArchitectureRule rule, Future<Throwable> pending) {
    try {
      return new RuleOutcome(rule, pending.get(), null);
    } catch (ExecutionException failed) {
      return new RuleOutcome(rule, null, asUnrecoverable(rule, failed.getCause()));
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new ArchitectureRunnerError(
          "Interrupted while verifying '%s', so the run is incomplete.".formatted(rule.name()), interrupted);
    }
  }

  private static Error asUnrecoverable(ArchitectureRule rule, Throwable cause) {
    if (cause instanceof Error error) {
      return error;
    }
    return new ArchitectureRunnerError(
        "Rule '%s' failed in a way that cannot be reported as a finding.".formatted(rule.name()), cause);
  }

  /**
   * Keeps the first unrecoverable error as the one that fails the build and attaches any later one to it, so
   * a run in which several rules cannot be evaluated still reports every reason.
   */
  private static Error keepFirst(Error first, Error next) {
    if (Objects.isNull(first)) {
      return next;
    }
    first.addSuppressed(next);
    return first;
  }

  private static Report toReport(Map<RuleSeverity, Integer> violated) {
    return new Report(
        violated.get(RuleSeverity.ERROR), violated.get(RuleSeverity.WARNING), violated.get(RuleSeverity.INFO));
  }

  /**
   * Emits the whole report as one log event on purpose: the layout prefix (timestamp, thread, logger) is
   * then printed once instead of on every line, so the report stays aligned and readable in a CI log. The
   * event level is the worst severity found, which keeps the report greppable by level.
   */
  private static void emit(List<String> lines, Report report, int evaluated, int total, RuleSeverity worst,
      long elapsedMillis) {
    int passed = evaluated - report.errors() - report.warnings() - report.infos();
    lines.add(SEPARATOR);
    lines.add("Architecture verification: %d passed, %d error(s), %d warning(s), %d info in %d ms"
        .formatted(passed, report.errors(), report.warnings(), report.infos(), elapsedMillis));
    if (evaluated < total) {
      lines.add("Run aborted: %d of %d rules were not evaluated, see the error below."
          .formatted(total - evaluated, total));
    }
    lines.add(SEPARATOR);
    LOG.log(Objects.isNull(worst) ? Level.INFO : worst.getLevel(),
        System.lineSeparator() + String.join(System.lineSeparator(), lines));
  }

  /**
   * Catches {@link AssertionError} because that is what {@link com.tngtech.archunit.lang.ArchRule} raises, and
   * every {@link Exception} so that a broken rule cannot hide the findings of the remaining rules. Any other
   * {@link Error}, such as {@link OutOfMemoryError} or a {@link LinkageError} from a partial classpath, is a
   * defect of the build itself rather than an architecture violation and is deliberately left to propagate: it
   * must never be reported under a rule severity and silently downgraded to a warning.
   */
  private static Throwable evaluate(ArchitectureRule rule, ArchitectureContext context) {
    try {
      rule.verify(context);
      return null;
    } catch (AssertionError | Exception violation) {
      return violation;
    }
  }

  /**
   * An {@link ArchitectureViolationException} and an ArchUnit {@link AssertionError} are deliberate, already
   * formatted findings, so only their message is printed. Anything else means the rule itself misbehaved, and
   * a bare message such as {@code "Cannot invoke ..."} is not debuggable from a CI log, so the stack trace is
   * kept.
   */
  private static String describe(Throwable violation) {
    String message = violation.getMessage();
    boolean intentionalFinding = violation instanceof ArchitectureViolationException
        || violation instanceof AssertionError;
    if (intentionalFinding && Objects.nonNull(message) && !message.isBlank()) {
      return message;
    }
    StringWriter trace = new StringWriter();
    violation.printStackTrace(new PrintWriter(trace));
    if (Objects.isNull(message) || message.isBlank()) {
      return "%s was thrown without a message:%s%s"
          .formatted(violation.getClass().getName(), System.lineSeparator(), trace);
    }
    return "%s%s%s".formatted(message, System.lineSeparator(), trace);
  }

  private static String indent(String message) {
    return message.lines()
        .map(CONTINUATION_INDENT::concat)
        .collect(Collectors.joining(System.lineSeparator()));
  }

  /**
   * Outcome of one rule evaluation.
   *
   * <p>Exactly one of {@code finding} and {@code unrecoverable} is set, or neither when the rule passed. A
   * finding is reported at the rule's severity; an unrecoverable error aborts the run.
   *
   * @param rule          the evaluated rule
   * @param finding       violation reported by the rule, or {@code null} when it is satisfied
   * @param unrecoverable error proving the rule could not run at all, or {@code null}
   */
  private record RuleOutcome(ArchitectureRule rule, Throwable finding, Error unrecoverable) {
  }

  /**
   * Aggregated outcome of a verification run.
   *
   * @param errors   number of violated error severity rules
   * @param warnings number of violated warning severity rules
   * @param infos    number of violated info severity rules
   */
  public record Report(int errors, int warnings, int infos) {

    /**
     * Tells whether the run produced a finding at or above the given severity.
     *
     * @param threshold least severe finding that counts
     * @return {@code true} when at least one finding reached the threshold
     */
    public boolean hasFindingAtOrAbove(RuleSeverity threshold) {
      for (RuleSeverity severity : RuleSeverity.values()) {
        if (severity.compareTo(threshold) >= 0 && countOf(severity) > 0) {
          return true;
        }
      }
      return false;
    }

    private int countOf(RuleSeverity severity) {
      if (severity == RuleSeverity.ERROR) {
        return errors;
      }
      if (severity == RuleSeverity.WARNING) {
        return warnings;
      }
      return infos;
    }
  }
}
