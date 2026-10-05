package dev.quokkify.architecture.rules;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.ArchitectureRule;
import dev.quokkify.architecture.contract.RuleSeverity;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

/**
 * Verifies that main code never writes to the console directly.
 *
 * <p>Library code that prints through {@code System.out}, {@code System.err}, {@code printStackTrace()} or
 * {@code Thread.dumpStack()} bypasses the logging configuration of the consumer: the output cannot be filtered,
 * routed or silenced, and a stack trace printed next to a log call is reported twice. Main code must log through
 * a logger instead.
 *
 * <p>The check reads method bodies, which bytecode does not preserve in a queryable form, so it runs on the
 * JavaParser model of {@link ArchitectureContext#mainSources()}. Test sources are not checked.
 *
 * <p>Any use of the {@code System.out} or {@code System.err} stream is reported, not only a call on it, so a
 * method reference such as {@code System.out::println} or {@code printStackTrace(System.err)} is caught too.
 * {@code printStackTrace} is reported without arguments only, because {@code printStackTrace(writer)} renders
 * into a target the caller chose.
 *
 * <p>Limitation: names are matched syntactically, without resolving symbols. A statically imported {@code out},
 * a {@code PrintStream} obtained elsewhere and the {@code System.console()} API are not detected.
 */
public class NoConsoleOutputRule implements ArchitectureRule {

  private static final Set<String> CONSOLE_STREAMS = Set.of("out", "err");
  private static final String SYSTEM = "System";
  private static final String PRINT_STACK_TRACE = "printStackTrace";
  private static final String DUMP_STACK = "dumpStack";
  private static final String THREAD = "Thread";
  private static final String EXPECTED_CONTRACT = """
      Main code must not write to the console through System.out, System.err, printStackTrace() or \
      Thread.dumpStack(): that output bypasses the consumer's logging configuration. Log through a Log4j or SLF4J \
      logger instead, passing the exception as the last argument to keep its stack trace.""";

  @Override
  public String name() {
    return "No console output in main code";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.ERROR;
  }

  @Override
  public void verify(ArchitectureContext context) {
    List<String> violations = context.mainSources().units().stream()
        .flatMap(NoConsoleOutputRule::violationsOf)
        .toList();
    checkViolations(EXPECTED_CONTRACT, violations);
  }

  private static Stream<String> violationsOf(CompilationUnit unit) {
    Stream<Node> streams = unit.findAll(FieldAccessExpr.class).stream()
        .filter(NoConsoleOutputRule::isConsoleStream)
        .map(Node.class::cast);
    Stream<Node> streamReferences = unit.findAll(MethodReferenceExpr.class).stream()
        .filter(NoConsoleOutputRule::referencesConsoleStream)
        .map(Node.class::cast);
    Stream<Node> stackTraces = unit.findAll(MethodCallExpr.class).stream()
        .filter(NoConsoleOutputRule::printsStackTrace)
        .map(Node.class::cast);
    return Stream.of(streams, streamReferences, stackTraces)
        .flatMap(found -> found)
        .sorted(Comparator.comparingInt(NoConsoleOutputRule::lineOf))
        .map(node -> "%s:%d %s".formatted(locationOf(unit, node), lineOf(node), describe(node)));
  }

  private static String describe(Node node) {
    if (node instanceof MethodCallExpr call) {
      return "calls %s()".formatted(call.getNameAsString());
    }
    if (node instanceof MethodReferenceExpr reference) {
      return "references System.%s::%s".formatted(streamOf(reference), reference.getIdentifier());
    }
    return "uses System." + ((FieldAccessExpr) node).getNameAsString();
  }

  /**
   * JavaParser cannot tell a field from a type in front of {@code ::}, so {@code System.out::println} arrives as
   * a type expression {@code System.out} rather than a field access.
   */
  private static boolean referencesConsoleStream(MethodReferenceExpr reference) {
    return !streamOf(reference).isEmpty();
  }

  private static String streamOf(MethodReferenceExpr reference) {
    Expression scope = reference.getScope();
    if (scope instanceof FieldAccessExpr access && isConsoleStream(access)) {
      return access.getNameAsString();
    }
    if (scope instanceof TypeExpr type && type.getType() instanceof ClassOrInterfaceType stream
        && CONSOLE_STREAMS.contains(stream.getNameAsString())
        && stream.getScope().map(NoConsoleOutputRule::isSystemType).orElse(false)) {
      return stream.getNameAsString();
    }
    return "";
  }

  private static boolean isSystemType(ClassOrInterfaceType type) {
    if (!SYSTEM.equals(type.getNameAsString())) {
      return false;
    }
    return type.getScope().map(scope -> "java.lang".equals(scope.getNameWithScope())).orElse(true);
  }

  private static boolean isConsoleStream(FieldAccessExpr access) {
    return CONSOLE_STREAMS.contains(access.getNameAsString()) && isSystem(access.getScope());
  }

  /**
   * Matches {@code System} and {@code java.lang.System} structurally rather than through {@code toString()},
   * which renders comments and lazily mutates the shared syntax tree.
   */
  private static boolean isSystem(Expression scope) {
    if (scope instanceof NameExpr name) {
      return SYSTEM.equals(name.getNameAsString());
    }
    return scope instanceof FieldAccessExpr qualified
        && SYSTEM.equals(qualified.getNameAsString())
        && qualified.getScope() instanceof FieldAccessExpr lang
        && "lang".equals(lang.getNameAsString())
        && lang.getScope() instanceof NameExpr java
        && "java".equals(java.getNameAsString());
  }

  private static boolean printsStackTrace(MethodCallExpr call) {
    boolean bareStackTrace = PRINT_STACK_TRACE.equals(call.getNameAsString()) && call.getArguments().isEmpty();
    boolean dumpStack = DUMP_STACK.equals(call.getNameAsString())
        && call.getScope().filter(scope -> scope instanceof NameExpr name
            && THREAD.equals(name.getNameAsString())).isPresent();
    return bareStackTrace || dumpStack;
  }

  private static String locationOf(CompilationUnit unit, Node node) {
    return node.findAncestor(TypeDeclaration.class)
        .flatMap(NoConsoleOutputRule::qualifiedName)
        .or(() -> unit.getStorage().map(storage -> storage.getPath().toString()))
        .orElse("<unknown>");
  }

  private static Optional<String> qualifiedName(TypeDeclaration<?> type) {
    return type.getFullyQualifiedName();
  }

  private static int lineOf(Node node) {
    return node.getBegin().map(position -> position.line).orElse(0);
  }
}
