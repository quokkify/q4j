package dev.quokkify.architecture.restassured;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.ArchitectureRule;
import dev.quokkify.architecture.contract.RuleScope;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

/**
 * Verifies that API tests pass HTTP status codes as {@code org.apache.http.HttpStatus} constants, such as
 * {@code HttpStatus.SC_OK}, never as number literals such as {@code 200}.
 *
 * <p>The compiler inlines {@code static final int} constants, so the bytecode of {@code HttpStatus.SC_OK} and
 * {@code 200} is identical. The rule therefore reads the test sources, {@link ArchitectureContext#testSources()}.
 *
 * <p>An API test is a test source whose package has a segment named after the marker, for example
 * {@code com.example.test.api}, or a class or method whose TestNG {@code @Test(groups = ...)} contains the marker
 * as a string literal. The marker is {@code api} by default; set {@value #MARKER_PROPERTY} or the
 * {@value #MARKER_VARIABLE} environment variable to change it. In an API test, a call to a method whose name ends
 * in {@code Status} or {@code StatusCode}, such as {@code verifyResponseStatusCode}, must not take an integer literal
 * between 100 and 599 as a direct argument.
 *
 * <p>Register the rule in the consumer's
 * {@code META-INF/services/dev.quokkify.architecture.contract.ArchitectureRule} when the project uses this
 * module; the runner classpath then holds {@code org.apache.http.HttpStatus} too.
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
  private static final String HTTP_STATUS = "org.apache.http.HttpStatus";
  private static final Pattern STATUS_METHOD = Pattern.compile("(?i).*status(code)?");
  private static final String TEST_ANNOTATION = "Test";
  private static final String GROUPS = "groups";
  private static final int LOWEST_STATUS = 100;
  private static final int HIGHEST_STATUS = 599;
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
    Map<Integer, String> constants = constants();
    List<String> violations = apiTests.stream()
        .flatMap(scope -> scope.findAll(MethodCallExpr.class).stream())
        .filter(call -> STATUS_METHOD.matcher(call.getNameAsString()).matches())
        .flatMap(call -> call.getArguments().stream()
            .filter(IntegerLiteralExpr.class::isInstance)
            .map(IntegerLiteralExpr.class::cast)
            .filter(HttpStatusConstantRule::isStatusCode)
            .map(literal -> Finding.of(call, literal)))
        .distinct()
        .sorted(Comparator.comparing(Finding::path).thenComparingInt(Finding::line).thenComparing(Finding::method))
        .map(finding -> finding.describe(constants))
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
    Stream<Node> classes = unit.findAll(ClassOrInterfaceDeclaration.class).stream()
        .filter(this::isMarked)
        .map(Node.class::cast);
    Stream<Node> methods = unit.findAll(MethodDeclaration.class).stream()
        .filter(this::isMarked)
        .filter(method -> method.findAncestor(ClassOrInterfaceDeclaration.class).filter(this::isMarked).isEmpty())
        .map(Node.class::cast);
    return Stream.concat(classes, methods);
  }

  private boolean isMarked(NodeWithAnnotations<?> declaration) {
    return declaration.getAnnotations().stream().anyMatch(this::marksApiTest);
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

  /**
   * Reads the constants reflectively, so a runner classpath without httpcore fails as a run that cannot verify
   * rather than as an error loading the rule. The alphabetically first name wins when two share a value.
   */
  private static Map<Integer, String> constants() {
    Class<?> httpStatus;
    try {
      httpStatus = Class.forName(HTTP_STATUS, false, HttpStatusConstantRule.class.getClassLoader());
    } catch (ClassNotFoundException | LinkageError missing) {
      throw new ArchitectureRunnerError(
          "%s is not on the runner classpath, so status literals cannot be mapped to constants.".formatted(HTTP_STATUS),
          missing);
    }
    Map<Integer, String> constants = new TreeMap<>();
    Arrays.stream(httpStatus.getFields())
        .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == int.class)
        .sorted(Comparator.comparing(Field::getName))
        .forEach(field -> constants.putIfAbsent(valueOf(field), field.getName()));
    return constants;
  }

  private static int valueOf(Field field) {
    try {
      return field.getInt(null);
    } catch (IllegalAccessException inaccessible) {
      throw new IllegalStateException(inaccessible);
    }
  }

  /**
   * One status literal passed to a status method.
   */
  private record Finding(String path, int line, String method, int code) {

    static Finding of(MethodCallExpr call, IntegerLiteralExpr literal) {
      String path = call.findCompilationUnit()
          .flatMap(CompilationUnit::getStorage)
          .map(storage -> storage.getPath().toString())
          .orElse("<unknown source>");
      return new Finding(path, literal.getBegin().map(position -> position.line).orElse(0), call.getNameAsString(),
          literal.asNumber().intValue());
    }

    String describe(Map<Integer, String> constants) {
      String constant = Optional.ofNullable(constants.get(code))
          .map(name -> "HttpStatus." + name)
          .orElse("an HttpStatus constant");
      return "%s line %d: %s(...) passes %d; use %s".formatted(path, line, method, code, constant);
    }
  }

  private static String configuredMarker() {
    String property = System.getProperty(MARKER_PROPERTY);
    String configured = Objects.nonNull(property) && !property.isBlank() ? property : System.getenv(MARKER_VARIABLE);
    return Objects.isNull(configured) || configured.isBlank() ? DEFAULT_MARKER : configured.trim();
  }
}
