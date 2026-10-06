package dev.quokkify.architecture.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.ArchitectureRule;
import dev.quokkify.architecture.contract.RuleScope;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.apache.http.HttpStatus;

/**
 * Verifies that API tests pass HTTP status codes as {@link HttpStatus} constants, such as {@code HttpStatus.SC_OK},
 * never as number literals such as {@code 200}.
 *
 * <p>The compiler inlines {@code static final int} constants, so the bytecode of {@code HttpStatus.SC_OK} and
 * {@code 200} is identical. The rule therefore reads the test sources, {@link ArchitectureContext#testSources()}.
 *
 * <p>An API test is a test source whose package has a segment named after the marker, for example
 * {@code com.example.test.api}, or a method whose TestNG {@code @Test(groups = ...)} contains the marker. The
 * marker is {@code api} by default; set {@value #MARKER_PROPERTY} or the {@value #MARKER_VARIABLE} environment
 * variable to change it. In an API test, a call to a method whose name contains {@code status}, such as
 * {@code verifyResponseStatusCode}, must not take an integer literal between 100 and 599.
 *
 * <p>Register the rule in the consumer's
 * {@code META-INF/services/dev.quokkify.architecture.contract.ArchitectureRule} when the project uses this
 * module; the runner classpath then holds {@link HttpStatus} too.
 */
public class HttpStatusConstantRule implements ArchitectureRule {

  /**
   * System property naming the API test marker, a package segment or TestNG group.
   */
  static final String MARKER_PROPERTY = "architecture.api.package";

  /**
   * Environment variable naming the API test marker, read when the system property is unset.
   */
  static final String MARKER_VARIABLE = "ARCHITECTURE_API_PACKAGE";

  private static final String DEFAULT_MARKER = "api";
  private static final String STATUS = "status";
  private static final String TEST_ANNOTATION = "Test";
  private static final String GROUPS = "groups";
  private static final int LOWEST_STATUS = 100;
  private static final int HIGHEST_STATUS = 599;
  private static final Map<Integer, String> CONSTANTS = constants();
  private static final String EXPECTED_CONTRACT = """
      API tests pass HTTP status codes as org.apache.http.HttpStatus constants, such as HttpStatus.SC_OK, never as \
      number literals: the constant names the expected outcome. Replace each literal with the constant shown.""";

  private final String marker;

  /**
   * Creates the rule with the marker from {@value #MARKER_PROPERTY}, {@value #MARKER_VARIABLE}, or {@code api}.
   */
  public HttpStatusConstantRule() {
    this(configuredMarker());
  }

  HttpStatusConstantRule(String marker) {
    this.marker = marker;
  }

  @Override
  public String name() {
    return "HTTP statuses use HttpStatus constants";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.ERROR;
  }

  @Override
  public Set<RuleScope> scopes() {
    return Set.of(RuleScope.TEST);
  }

  @Override
  public void verify(ArchitectureContext context) {
    List<CompilationUnit> units = context.testSources().units();
    if (units.isEmpty()) {
      return;
    }
    List<Node> apiTests = units.stream().flatMap(this::apiTestsOf).toList();
    if (apiTests.isEmpty()) {
      throw new ArchitectureRunnerError("""
          No test source lies in a package segment '%s' or declares @Test(groups = "%s"), so no API test was \
          verified. Set -D%s or %s to the marker of this project's API tests.\
          """.formatted(marker, marker, MARKER_PROPERTY, MARKER_VARIABLE));
    }
    List<String> violations = apiTests.stream()
        .flatMap(scope -> scope.findAll(MethodCallExpr.class).stream())
        .filter(call -> call.getNameAsString().toLowerCase(Locale.ROOT).contains(STATUS))
        .flatMap(call -> call.getArguments().stream()
            .filter(IntegerLiteralExpr.class::isInstance)
            .map(IntegerLiteralExpr.class::cast)
            .filter(HttpStatusConstantRule::isStatusCode)
            .map(literal -> describe(call, literal)))
        .distinct()
        .sorted()
        .toList();
    checkViolations(EXPECTED_CONTRACT, violations);
  }

  private Stream<Node> apiTestsOf(CompilationUnit unit) {
    boolean inApiPackage = unit.getPackageDeclaration()
        .map(PackageDeclaration::getNameAsString)
        .map(name -> Arrays.asList(name.split("\\.")).contains(marker))
        .orElse(false);
    if (inApiPackage) {
      return Stream.of(unit);
    }
    return unit.findAll(MethodDeclaration.class).stream()
        .filter(method -> method.getAnnotations().stream().anyMatch(this::marksApiTest))
        .map(Node.class::cast);
  }

  private boolean marksApiTest(AnnotationExpr annotation) {
    if (!(annotation instanceof NormalAnnotationExpr test)
        || !annotation.getName().getIdentifier().equals(TEST_ANNOTATION)) {
      return false;
    }
    return test.getPairs().stream()
        .filter(pair -> pair.getNameAsString().equals(GROUPS))
        .flatMap(pair -> pair.getValue().findAll(StringLiteralExpr.class).stream())
        .anyMatch(group -> group.asString().equals(marker));
  }

  private static boolean isStatusCode(IntegerLiteralExpr literal) {
    long code = literal.asNumber().longValue();
    return code >= LOWEST_STATUS && code <= HIGHEST_STATUS;
  }

  private static String describe(MethodCallExpr call, IntegerLiteralExpr literal) {
    String location = call.findCompilationUnit()
        .flatMap(CompilationUnit::getStorage)
        .map(storage -> storage.getPath().toString())
        .orElse("<unknown source>");
    int line = literal.getBegin().map(position -> position.line).orElse(0);
    int code = literal.asNumber().intValue();
    String constant = Optional.ofNullable(CONSTANTS.get(code))
        .map(name -> "HttpStatus." + name)
        .orElse("an HttpStatus constant");
    return "%s line %d: %s(...) passes %d; use %s".formatted(location, line, call.getNameAsString(), code, constant);
  }

  private static Map<Integer, String> constants() {
    return Arrays.stream(HttpStatus.class.getFields())
        .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == int.class)
        .collect(Collectors.toMap(HttpStatusConstantRule::valueOf, Field::getName, (first, ignored) -> first));
  }

  private static int valueOf(Field field) {
    try {
      return field.getInt(null);
    } catch (IllegalAccessException inaccessible) {
      throw new IllegalStateException(inaccessible);
    }
  }

  private static String configuredMarker() {
    String property = System.getProperty(MARKER_PROPERTY);
    String configured = Objects.nonNull(property) && !property.isBlank() ? property : System.getenv(MARKER_VARIABLE);
    return Objects.isNull(configured) || configured.isBlank() ? DEFAULT_MARKER : configured.trim();
  }
}
