package dev.quokkify.architecture.rules;

import java.util.List;

import dev.quokkify.architecture.ClassDirs;
import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.fixtures.console.violating.NoisyComponent;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NoConsoleOutputRuleTest {

  private static final String FIXTURES = "dev.quokkify.architecture.fixtures.console.";

  private final NoConsoleOutputRule rule = new NoConsoleOutputRule();

  @Test
  public void everyConsoleWriteIsReported() {
    try (ArchitectureContext context = mainContextFor("violating")) {
      assertThatThrownBy(() -> rule.verify(context))
          .isInstanceOf(AssertionError.class)
          .as("System.out, a statically imported out, System.err::println, printStackTrace() and dumpStack()")
          .hasMessageContaining("Found 5 Taikai violation(s)")
          .hasMessageContaining(NoisyComponent.class.getName() + ".run() calls java.lang.System.out")
          .hasMessageContaining(NoisyComponent.class.getName() + ".run() calls java.lang.System.err")
          .hasMessageContaining("java.lang.IllegalStateException.printStackTrace()")
          .hasMessageContaining("java.lang.Thread.dumpStack()");
    }
  }

  @Test
  public void stackTraceRenderedIntoAChosenWriterIsAllowed() {
    try (ArchitectureContext context = mainContextFor("clean")) {
      assertThatCode(() -> rule.verify(context)).doesNotThrowAnyException();
    }
  }

  @Test
  public void testClassesAreNotVerified() {
    try (ArchitectureContext context = ArchitectureContext.builder(List.of(FIXTURES + "violating"))
        .mainClasses(List.of())
        .testClasses(List.of(ClassDirs.test()))
        .build()) {
      assertThatCode(() -> rule.verify(context))
          .as("a project without main classes has nothing to verify, even if its tests print")
          .doesNotThrowAnyException();
    }
  }

  @Test
  public void noConsoleOutputIsMandatory() {
    assertThat(rule.severity()).isEqualTo(RuleSeverity.ERROR);
  }

  /**
   * The fixtures are compiled into the test class directory, which therefore plays the main directory here.
   */
  private static ArchitectureContext mainContextFor(String fixture) {
    return ArchitectureContext.builder(List.of(FIXTURES + fixture))
        .mainClasses(List.of(ClassDirs.test()))
        .testClasses(List.of())
        .build();
  }
}
