package dev.quokkify.architecture.contract;

import java.nio.file.Path;
import java.util.List;

import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArchitectureContextTest {

  @Test
  public void packageListIsTrimmedAndBlankEntriesAreDropped() {
    assertThat(ArchitectureContext.parsePackages(" dev.quokkify , com.example,, ")).containsExactly(
        "dev.quokkify", "com.example");
    assertThat(ArchitectureContext.parsePackages(null)).isEmpty();
  }

  @Test
  public void absentSourcePropertyLeavesTheGroupUnconfigured() {
    assertThat(ArchitectureContext.parseRoots(null)).isNull();
    assertThat(ArchitectureContext.parseRoots("")).isEmpty();
    assertThat(ArchitectureContext.parseRoots("src/main/java, build/x")).containsExactly(
        Path.of("src/main/java"), Path.of("build/x"));
  }

  @Test
  public void missingSourceRootCannotBeVerified() {
    try (ArchitectureContext context = new ArchitectureContext(
        List.of("dev.quokkify"), List.of(Path.of("does/not/exist")), List.of())) {
      assertThatThrownBy(() -> context.mainSources().units())
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining("does not exist");
    }
  }

  @Test
  public void contextWithoutPackagesCannotRun() {
    assertThatThrownBy(() -> new ArchitectureContext(List.of()))
        .as("a context covering nothing would let every rule pass without verifying anything")
        .isInstanceOf(ArchitectureRunnerError.class)
        .hasMessageContaining(ArchitectureContext.PACKAGES_PROPERTY);
  }

  @Test
  public void closedContextRefusesToScanAgain() {
    ArchitectureContext context = new ArchitectureContext(List.of("dev.quokkify.architecture.fixtures"));
    context.close();

    assertThatThrownBy(context::scan).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(context::all).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(context::mainSources).isInstanceOf(IllegalStateException.class);
  }
}
