package dev.quokkify.architecture.rules;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;
import dev.quokkify.architecture.exceptions.ArchitectureViolationException;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NoConsoleOutputRuleTest {

  private static final List<String> PACKAGES = List.of("dev.quokkify");

  private final NoConsoleOutputRule rule = new NoConsoleOutputRule();

  @Test
  public void everyConsoleWriteIsReportedWithItsLocation() {
    try (ArchitectureContext context = contextFor("violating")) {
      assertThatThrownBy(() -> rule.verify(context))
          .isInstanceOf(ArchitectureViolationException.class)
          .hasMessageContaining("Violations (6)")
          .hasMessageContaining("dev.quokkify.sample.NoisyService:8 uses System.out")
          .hasMessageContaining("dev.quokkify.sample.NoisyService:12 uses System.err")
          .hasMessageContaining("dev.quokkify.sample.NoisyService:13 calls printStackTrace()")
          .hasMessageContaining("dev.quokkify.sample.NoisyService:14 uses System.err")
          .hasMessageContaining("dev.quokkify.sample.NoisyService:16 references System.out::println")
          .hasMessageContaining("dev.quokkify.sample.NoisyService:17 calls dumpStack()");
    }
  }

  @Test
  public void stackTraceRenderedIntoAChosenWriterIsAllowed() {
    try (ArchitectureContext context = contextFor("clean")) {
      assertThatCode(() -> rule.verify(context)).doesNotThrowAnyException();
      assertThat(context.mainSources().units()).as("the clean source must be read, not missed").hasSize(1);
    }
  }

  @Test
  public void sourcesOfWhichNoneLiesUnderTheVerifiedPackagesCannotBeVerified() {
    try (ArchitectureContext context = contextFor("foreign")) {
      assertThatThrownBy(() -> rule.verify(context))
          .as("filtering every source away must not pass as a clean result")
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining("none declares a package under [dev.quokkify]");
    }
  }

  @Test
  public void sourcesOutsideTheVerifiedPackagesAreIgnoredNextToVerifiedOnes() {
    try (ArchitectureContext context = new ArchitectureContext(
        PACKAGES, List.of(sourceRoot("clean"), sourceRoot("foreign")), List.of())) {
      assertThatCode(() -> rule.verify(context)).doesNotThrowAnyException();
      assertThat(context.mainSources().units()).hasSize(1);
    }
  }

  @Test
  public void unparseableSourceMeansTheRuleCannotRun() {
    try (ArchitectureContext context = contextFor("broken")) {
      assertThatThrownBy(() -> rule.verify(context))
          .as("a file the parser could not read must not be reported as clean")
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining("Broken.java");
    }
  }

  @Test
  public void unconfiguredMainSourcesMeanTheRuleCannotRun() {
    try (ArchitectureContext context = new ArchitectureContext(PACKAGES)) {
      assertThatThrownBy(() -> rule.verify(context))
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining(ArchitectureContext.MAIN_SOURCES_PROPERTY);
    }
  }

  @Test
  public void projectWithoutMainSourcesPasses() {
    try (ArchitectureContext context = new ArchitectureContext(PACKAGES, List.of(), List.of())) {
      assertThatCode(() -> rule.verify(context)).doesNotThrowAnyException();
    }
  }

  @Test
  public void noConsoleOutputIsMandatory() {
    assertThat(rule.severity()).isEqualTo(RuleSeverity.ERROR);
  }

  private static ArchitectureContext contextFor(String fixture) {
    return new ArchitectureContext(PACKAGES, List.of(sourceRoot(fixture)), List.of());
  }

  private static Path sourceRoot(String fixture) {
    try {
      return Path.of(Objects.requireNonNull(
          NoConsoleOutputRuleTest.class.getResource("/sources/" + fixture), fixture).toURI());
    } catch (URISyntaxException invalid) {
      throw new IllegalStateException(invalid);
    }
  }
}
