package dev.quokkify.architecture;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import dev.quokkify.architecture.contract.ModelClock.ModelTime;
import dev.quokkify.architecture.contract.RuleScope;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

/**
 * Renders the report of {@link ArchitectureRunner} as plain text for logs or as a colored block for terminals.
 *
 * <p>The plain layout keeps a bracketed tag per rule, such as {@code [PASS]} or {@code [ERROR]}, so a CI log stays
 * greppable and free of escape sequences. The rich layout adds ANSI colors and status symbols; it falls back to
 * ASCII symbols when standard output is not UTF-8, so a Windows code page never prints question marks.
 */
final class ReportFormat {

  /**
   * Whether the report is colored: {@code auto} (the default) colors it when the runner writes to a terminal,
   * {@code always} and {@code never} force the choice. A build tool that pipes the runner output, as Gradle
   * does, decides for it.
   */
  static final String COLOR_PROPERTY = "architecture.color";

  /**
   * Optional name of the verified module, such as a Gradle project path, shown in the report header.
   */
  static final String MODULE_PROPERTY = "architecture.module";

  private static final String AUTO = "auto";
  private static final String ALWAYS = "always";
  private static final String NEVER = "never";

  private static final String SEPARATOR = "=".repeat(60);
  private static final String PLAIN_INDENT = "    ";
  private static final String TAG_FORMAT = "%-7s";
  private static final int RICH_RULE_WIDTH = 72;
  private static final String UNDECLARED_SCOPE = "-";
  private static final String MODELS_HEADER = "Shared models, built once (rule times below exclude them):";

  private static final String RESET = "\u001B[0m";
  private static final String BOLD = "\u001B[1m";
  private static final String DIM = "\u001B[2m";
  private static final String RED = "\u001B[31m";
  private static final String GREEN = "\u001B[32m";
  private static final String YELLOW = "\u001B[33m";
  private static final String BLUE = "\u001B[34m";
  private static final String CYAN = "\u001B[36m";

  private final String module;
  private final boolean color;
  private final Symbols symbols;

  ReportFormat(String module, boolean color, boolean unicode) {
    this.module = Objects.isNull(module) ? "" : module.trim();
    this.color = color;
    this.symbols = unicode ? Symbols.UNICODE : Symbols.ASCII;
  }

  /**
   * Reads the format from {@link #COLOR_PROPERTY}, {@link #MODULE_PROPERTY} and the standard output encoding.
   *
   * @return the format the current process prints its report in
   */
  static ReportFormat fromSystemProperties() {
    boolean terminal = Objects.nonNull(System.console())
        && isBlank(System.getenv("NO_COLOR"))
        && !"dumb".equals(System.getenv("TERM"));
    return new ReportFormat(
        System.getProperty(MODULE_PROPERTY),
        parseColor(System.getProperty(COLOR_PROPERTY, AUTO), terminal),
        isUtf8(System.getProperty("stdout.encoding", Charset.defaultCharset().name())));
  }

  /**
   * Parses {@link #COLOR_PROPERTY}, kept separate from the system property so it can be tested without
   * touching global state.
   *
   * @param configured {@code auto}, {@code always} or {@code never}, case insensitive
   * @param terminal   whether the output is a terminal, the answer for {@code auto}
   * @return whether the report is colored
   */
  static boolean parseColor(String configured, boolean terminal) {
    String value = Objects.isNull(configured) ? "" : configured.trim().toLowerCase(Locale.ENGLISH);
    return switch (value) {
      case AUTO -> terminal;
      case ALWAYS -> true;
      case NEVER -> false;
      default -> throw new ArchitectureRunnerError(
          "Unknown value '%s' for -D%s. Use auto, always or never.".formatted(configured, COLOR_PROPERTY));
    };
  }

  private static boolean isBlank(String value) {
    return Objects.isNull(value) || value.isBlank();
  }

  private static boolean isUtf8(String encoding) {
    try {
      return StandardCharsets.UTF_8.equals(Charset.forName(encoding));
    } catch (IllegalArgumentException unknown) {
      return false;
    }
  }

  /**
   * Renders the whole report block.
   *
   * @param total   number of rules that were to be evaluated
   * @param models  build time of each shared model, slowest first
   * @param rows    the evaluated rules, in report order
   * @param report  aggregated findings
   * @param elapsed wall clock time of the whole run, in milliseconds
   * @return the report lines
   */
  List<String> render(int total, List<ModelTime> models, List<Row> rows, ArchitectureRunner.Report report,
      long elapsed) {
    return color ? rich(total, models, rows, report, elapsed) : plain(total, models, rows, report, elapsed);
  }

  /**
   * Renders the gate decision that follows the report.
   *
   * @param failOn  the threshold, or empty when the gate never fails
   * @param failing whether a finding reached the threshold
   * @return the gate line
   */
  String gate(Optional<RuleSeverity> failOn, boolean failing) {
    String threshold = failOn.map(RuleSeverity::name).orElse(NEVER.toUpperCase(Locale.ENGLISH));
    if (!color) {
      return "Gate: fail on %s -> %s".formatted(threshold, failing ? "failed" : "passed");
    }
    String verdict = failing
        ? paint(RED + BOLD, symbols.failed() + " Gate failed")
        : paint(GREEN + BOLD, symbols.passed() + " Gate passed");
    return "  %s %s".formatted(verdict, paint(DIM, "(fail on %s)".formatted(threshold)));
  }

  private List<String> plain(int total, List<ModelTime> models, List<Row> rows, ArchitectureRunner.Report report,
      long elapsed) {
    int nameWidth = nameWidth(rows);
    int scopeWidth = scopeWidth(rows);
    int timeWidth = timeWidth(rows);
    List<String> lines = new ArrayList<>();
    lines.add(SEPARATOR);
    lines.add(module.isEmpty()
        ? "Architecture verification (%d rules)".formatted(total)
        : "Architecture verification of %s (%d rules)".formatted(module, total));
    lines.add(SEPARATOR);
    if (!models.isEmpty()) {
      int modelWidth = modelWidth(models);
      int modelTimeWidth = modelTimeWidth(models);
      lines.add(MODELS_HEADER);
      models.forEach(model -> lines.add("  %s  %s".formatted(
          pad(model.name(), modelWidth), duration(model.millis(), modelTimeWidth))));
      lines.add("-".repeat(SEPARATOR.length()));
    }
    for (Row row : rows) {
      String tag = row.passed() ? "[PASS]" : "[%s]".formatted(row.severity().getLabel());
      lines.add("%s %s  %s  %-7s  %s".formatted(TAG_FORMAT.formatted(tag), pad(row.name(), nameWidth),
          pad(row.scope(), scopeWidth), row.severity().name(), duration(row.millis(), timeWidth)).stripTrailing());
      if (!row.passed()) {
        row.message().lines().map(PLAIN_INDENT::concat).forEach(lines::add);
      }
    }
    lines.add(SEPARATOR);
    lines.add("Architecture verification: %d passed, %d error(s), %d warning(s), %d info in %d ms"
        .formatted(passed(rows, report), report.errors(), report.warnings(), report.infos(), elapsed));
    if (rows.size() < total) {
      lines.add(aborted(total, rows));
    }
    lines.add(SEPARATOR);
    return lines;
  }

  private List<String> rich(int total, List<ModelTime> models, List<Row> rows, ArchitectureRunner.Report report,
      long elapsed) {
    int nameWidth = nameWidth(rows);
    int scopeWidth = scopeWidth(rows);
    int timeWidth = timeWidth(rows);
    String dot = " %s ".formatted(symbols.dot());
    List<String> lines = new ArrayList<>();
    String scope = module.isEmpty() ? "" : paint(BOLD, module) + paint(DIM, dot);
    lines.add(" %s  %s%s".formatted(
        paint(CYAN + BOLD, "Architecture"), scope, paint(DIM, "%d rules".formatted(total))));
    lines.add("");
    if (!models.isEmpty()) {
      int modelWidth = modelWidth(models);
      int modelTimeWidth = modelTimeWidth(models);
      lines.add("  " + paint(DIM, MODELS_HEADER));
      models.forEach(model -> lines.add("    %s  %s".formatted(
          pad(model.name(), modelWidth), paint(DIM, duration(model.millis(), modelTimeWidth)))));
      lines.add("");
    }
    for (Row row : rows) {
      String accent = accent(row);
      String name = row.passed() ? pad(row.name(), nameWidth) : paint(accent + BOLD, pad(row.name(), nameWidth));
      lines.add("  %s %s  %s  %s  %s".formatted(paint(accent, symbol(row)), name,
          paint(BLUE, pad(row.scope(), scopeWidth)), paint(DIM, "%-7s".formatted(row.severity().name())),
          paint(DIM, duration(row.millis(), timeWidth))));
      if (!row.passed()) {
        row.message().lines()
            .map(line -> "    %s %s".formatted(paint(accent, symbols.bar()), line))
            .forEach(lines::add);
      }
    }
    lines.add(paint(DIM, "  " + symbols.rule().repeat(Math.min(RICH_RULE_WIDTH, nameWidth + scopeWidth + timeWidth + 18))));
    List<String> counts = List.of(
        count(GREEN, passed(rows, report), "passed", "passed"),
        count(RED, report.errors(), "error", "errors"),
        count(YELLOW, report.warnings(), "warning", "warnings"),
        count(CYAN, report.infos(), "info", "info"),
        paint(DIM, "%d ms".formatted(elapsed)));
    lines.add("  " + String.join(paint(DIM, dot), counts));
    if (rows.size() < total) {
      lines.add("  " + paint(RED + BOLD, symbols.failed() + " " + aborted(total, rows)));
    }
    return lines;
  }

  private String count(String accent, int value, String singular, String plural) {
    String text = "%d %s".formatted(value, value == 1 ? singular : plural);
    return value == 0 ? paint(DIM, text) : paint(accent + BOLD, text);
  }

  private String symbol(Row row) {
    if (row.passed()) {
      return symbols.passed();
    }
    return switch (row.severity()) {
      case ERROR -> symbols.failed();
      case WARNING -> symbols.warning();
      case INFO -> symbols.info();
    };
  }

  private static String accent(Row row) {
    if (row.passed()) {
      return GREEN;
    }
    return switch (row.severity()) {
      case ERROR -> RED;
      case WARNING -> YELLOW;
      case INFO -> CYAN;
    };
  }

  private String paint(String style, String text) {
    return color ? style + text + RESET : text;
  }

  private static String aborted(int total, List<Row> rows) {
    return "Run aborted: %d of %d rules were not evaluated, see the error below."
        .formatted(total - rows.size(), total);
  }

  private static int passed(List<Row> rows, ArchitectureRunner.Report report) {
    return rows.size() - report.errors() - report.warnings() - report.infos();
  }

  private static int nameWidth(List<Row> rows) {
    return rows.stream().mapToInt(row -> row.name().length()).max().orElse(0);
  }

  private static int modelWidth(List<ModelTime> models) {
    return models.stream().mapToInt(model -> model.name().length()).max().orElse(0);
  }

  private static int modelTimeWidth(List<ModelTime> models) {
    return models.stream().mapToInt(model -> Long.toString(model.millis()).length()).max().orElse(1);
  }

  private static int scopeWidth(List<Row> rows) {
    return rows.stream().mapToInt(row -> row.scope().length()).max().orElse(0);
  }

  private static int timeWidth(List<Row> rows) {
    return rows.stream().mapToInt(row -> Long.toString(row.millis()).length()).max().orElse(1);
  }

  private static String pad(String text, int width) {
    return text + " ".repeat(Math.max(0, width - text.length()));
  }

  private static String duration(long millis, int width) {
    return ("%" + width + "d ms").formatted(millis);
  }

  /**
   * One evaluated rule as the report shows it.
   *
   * @param name     rule name
   * @param severity declared rule severity
   * @param scopes   declared verified scopes, empty when undeclared
   * @param message  the finding, or {@code null} when the rule passed
   * @param millis   time the rule took, without building or waiting for shared models, in milliseconds
   */
  record Row(String name, RuleSeverity severity, Set<RuleScope> scopes, String message, long millis) {

    boolean passed() {
      return Objects.isNull(message);
    }

    /**
     * Joins the scopes in declaration order, for example {@code MAIN+TEST}, whatever order the rule returned.
     */
    String scope() {
      if (Objects.isNull(scopes) || scopes.isEmpty()) {
        return UNDECLARED_SCOPE;
      }
      return scopes.stream().sorted().map(RuleScope::name).collect(Collectors.joining("+"));
    }
  }

  private record Symbols(String passed, String failed, String warning, String info, String bar, String dot,
      String rule) {

    private static final Symbols UNICODE = new Symbols("✔", "✘", "⚠", "ℹ", "│", "·", "─");
    private static final Symbols ASCII = new Symbols("+", "x", "!", "i", "|", "-", "-");
  }
}
