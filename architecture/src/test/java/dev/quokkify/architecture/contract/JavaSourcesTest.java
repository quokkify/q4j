package dev.quokkify.architecture.contract;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JavaSourcesTest {

  private static final List<String> PACKAGES = List.of("dev.quokkify");

  @Test
  public void sourcesUnderTheVerifiedPackagesAreParsed() {
    try (ArchitectureContext context = contextFor(sourceRoot("clean"))) {
      assertThat(context.mainSources().units()).hasSize(1);
    }
  }

  @Test
  public void sourcesOutsideTheVerifiedPackagesAreIgnoredNextToVerifiedOnes() {
    try (ArchitectureContext context = contextFor(sourceRoot("clean"), sourceRoot("foreign"))) {
      assertThat(context.mainSources().units()).hasSize(1);
    }
  }

  @Test
  public void sourcesOfWhichNoneLiesUnderTheVerifiedPackagesCannotBeVerified() {
    try (ArchitectureContext context = contextFor(sourceRoot("foreign"))) {
      assertThatThrownBy(() -> context.mainSources().units())
          .as("filtering every source away must not pass as a clean result")
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining("none declares a package under [dev.quokkify]");
    }
  }

  @Test
  public void unparseableSourceCannotBeVerified() {
    try (ArchitectureContext context = contextFor(sourceRoot("broken"))) {
      assertThatThrownBy(() -> context.mainSources().units())
          .as("a file the parser could not read must not be reported as clean")
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining("Broken.java");
    }
  }

  @Test
  public void missingSourceRootCannotBeVerified() {
    try (ArchitectureContext context = contextFor(Path.of("does/not/exist"))) {
      assertThatThrownBy(() -> context.mainSources().units())
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining("does not exist");
    }
  }

  @Test
  public void unconfiguredSourcesCannotBeRead() {
    try (ArchitectureContext context = new ArchitectureContext(PACKAGES)) {
      assertThat(context.mainSources().isConfigured()).isFalse();
      assertThatThrownBy(() -> context.mainSources().units())
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining(ArchitectureContext.MAIN_SOURCES_PROPERTY);
    }
  }

  @Test
  public void projectWithoutSourcesHasNoUnits() {
    try (ArchitectureContext context = ArchitectureContext.builder(PACKAGES).mainSources(List.of()).build()) {
      assertThat(context.mainSources().units()).isEmpty();
    }
  }

  private static ArchitectureContext contextFor(Path... roots) {
    return ArchitectureContext.builder(PACKAGES).mainSources(List.of(roots)).testSources(List.of()).build();
  }

  private static Path sourceRoot(String fixture) {
    try {
      return Path.of(Objects.requireNonNull(
          JavaSourcesTest.class.getResource("/sources/" + fixture), fixture).toURI());
    } catch (URISyntaxException invalid) {
      throw new IllegalStateException(invalid);
    }
  }
}
