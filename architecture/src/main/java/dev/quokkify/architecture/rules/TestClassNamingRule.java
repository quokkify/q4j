package dev.quokkify.architecture.rules;

import java.util.List;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.ArchitectureRule;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import io.github.classgraph.ClassInfo;

/**
 * Verifies that every class declaring TestNG tests is named {@code *Test}.
 *
 * <p>Test runners usually select classes by name pattern, Maven Surefire among them, so a class that declares
 * {@code @Test} methods but is not named {@code *Test} can be compiled and silently never executed. That
 * failure mode is invisible: the build stays green and the coverage is simply missing.
 *
 * <p>Support classes such as {@code *Steps} and {@code *Base} helpers declare no test methods and are therefore
 * not affected. The compiled test classes must be on the verification classpath; when no class declaring
 * {@code @Test} is found at all, the rule cannot run and aborts the verification, unless the context states
 * through {@link ArchitectureContext#testSources()} that the project has no test sources.
 */
public class TestClassNamingRule implements ArchitectureRule {

  private static final String TEST_ANNOTATION = "org.testng.annotations.Test";
  private static final String TEST_CLASS_SUFFIX = "Test";
  private static final String EXPECTED_CONTRACT = """
      Every class declaring TestNG @Test methods must have a name ending in 'Test', otherwise a name based test \
      selection does not pick it up and its tests never run. Rename the class, or move the shared code into a \
      support class without @Test methods.""";

  @Override
  public String name() {
    return "Test class naming";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.ERROR;
  }

  @Override
  public void verify(ArchitectureContext context) {
    List<ClassInfo> testClasses = context.scan().getAllClasses().stream()
        .filter(TestClassNamingRule::isVerifiable)
        .filter(TestClassNamingRule::declaresTest)
        .toList();
    if (testClasses.isEmpty() && hasNoTestSources(context)) {
      return;
    }
    if (testClasses.isEmpty()) {
      // Not a violation but an inability to verify, so it must fail the build regardless of this severity.
      throw new ArchitectureRunnerError("""
          No class declaring TestNG @Test was found under %s, so test naming was not verified at all. Add the \
          compiled test classes to the verification classpath.""".formatted(context.packages()));
    }
    List<String> violations = testClasses.stream()
        .filter(testClass -> !testClass.getSimpleName().endsWith(TEST_CLASS_SUFFIX))
        .map(testClass -> "%s declares @Test but is not named *%s".formatted(testClass.getName(), TEST_CLASS_SUFFIX))
        .toList();
    checkViolations(EXPECTED_CONTRACT, violations);
  }

  private static boolean hasNoTestSources(ArchitectureContext context) {
    return context.testSources().isConfigured() && context.testSources().roots().isEmpty();
  }

  private static boolean isVerifiable(ClassInfo testClass) {
    return !testClass.isSynthetic() && !testClass.isAnonymousInnerClass() && !testClass.isAbstract();
  }

  /**
   * TestNG accepts {@code @Test} on a method and on the class, where it applies to every public method, so
   * both placements have to be inspected.
   */
  private static boolean declaresTest(ClassInfo testClass) {
    return testClass.hasAnnotation(TEST_ANNOTATION)
        || testClass.getDeclaredMethodInfo().stream().anyMatch(method -> method.hasAnnotation(TEST_ANNOTATION));
  }
}
