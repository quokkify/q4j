package dev.quokkify.architecture.contract;

import java.nio.file.Path;
import java.util.List;

import dev.quokkify.architecture.ArchitectureRunner;
import dev.quokkify.architecture.ClassDirs;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import com.tngtech.archunit.core.domain.JavaClass;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArchitectureContextTest {

  private static final List<String> PACKAGES = List.of("dev.quokkify.architecture");

  @Test
  public void packageListIsTrimmedAndBlankEntriesAreDropped() {
    assertThat(ArchitectureContext.parsePackages(" dev.quokkify , com.example,, ")).containsExactly(
        "dev.quokkify", "com.example");
    assertThat(ArchitectureContext.parsePackages(null)).isEmpty();
  }

  @Test
  public void absentPathPropertyLeavesTheGroupUnconfigured() {
    assertThat(ArchitectureContext.parseRoots(null)).isNull();
    assertThat(ArchitectureContext.parseRoots("")).isEmpty();
    assertThat(ArchitectureContext.parseRoots("src/main/java, build/x")).containsExactly(
        Path.of("src/main/java"), Path.of("build/x"));
  }

  @Test
  public void mainAndTestClassesAreSeparatedByTheirClassDirectory() {
    try (ArchitectureContext context = ArchitectureContext.builder(PACKAGES)
        .mainClasses(List.of(ClassDirs.main()))
        .testClasses(List.of(ClassDirs.test()))
        .build()) {
      assertThat(names(context.mainClasses()))
          .contains(ArchitectureRunner.class.getName())
          .doesNotContain(ArchitectureContextTest.class.getName());
      assertThat(names(context.testClasses()))
          .contains(ArchitectureContextTest.class.getName())
          .doesNotContain(ArchitectureRunner.class.getName());
    }
  }

  @Test
  public void configuredClassDirectoriesKeepTheRestOfTheClasspathOut() {
    try (ArchitectureContext context = ArchitectureContext.builder(PACKAGES)
        .mainClasses(List.of())
        .testClasses(List.of(ClassDirs.test()))
        .build()) {
      assertThat(names(context.all()))
          .as("the runner is on the classpath but outside the configured directories")
          .doesNotContain(ArchitectureRunner.class.getName())
          .contains(ArchitectureContextTest.class.getName());
      assertThat(context.scan().getAllClasses().getNames()).doesNotContain(ArchitectureRunner.class.getName());
      assertThat(context.mainClasses()).isEmpty();
    }
  }

  @Test
  public void unconfiguredClassDirectoriesCannotSeparateMainFromTest() {
    try (ArchitectureContext context = new ArchitectureContext(PACKAGES)) {
      assertThatThrownBy(context::mainClasses)
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining(ArchitectureContext.MAIN_CLASSES_PROPERTY);
      assertThatThrownBy(context::testClasses)
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining(ArchitectureContext.TEST_CLASSES_PROPERTY);
      assertThat(names(context.all())).contains(ArchitectureRunner.class.getName());
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

  private static List<String> names(Iterable<JavaClass> classes) {
    return java.util.stream.StreamSupport.stream(classes.spliterator(), false).map(JavaClass::getName).toList();
  }
}
