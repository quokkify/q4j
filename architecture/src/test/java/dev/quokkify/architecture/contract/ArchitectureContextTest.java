package dev.quokkify.architecture.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import dev.quokkify.architecture.ArchitectureRunner;
import dev.quokkify.architecture.ClassDirs;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;
import dev.quokkify.architecture.fixtures.services.PoliteGreeting;
import dev.quokkify.architecture.fixtures.taikai.clean.QuietLogging;
import dev.quokkify.architecture.fixtures.taikai.violating.LowercaseLogger;

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
  public void classesWithoutAnAuthoredSourceAreLeftOutLikeGeneratedCode() {
    Path onlyQuietLogging = Path.of("src/test/java/dev/quokkify/architecture/fixtures/taikai/clean");
    try (ArchitectureContext context = ArchitectureContext.builder(List.of("dev.quokkify.architecture.fixtures.taikai"))
        .mainClasses(List.of())
        .testClasses(List.of(ClassDirs.test()))
        .mainSources(List.of())
        .testSources(List.of(onlyQuietLogging))
        .build()) {
      assertThat(names(context.all()))
          .as("LowercaseLogger is compiled but has no source under the roots, as generated code would")
          .contains(QuietLogging.class.getName())
          .doesNotContain(LowercaseLogger.class.getName());
    }
  }

  @Test
  public void withoutSourcesEveryCompiledClassIsKept() {
    try (ArchitectureContext context = ArchitectureContext.builder(List.of("dev.quokkify.architecture.fixtures.taikai"))
        .mainClasses(List.of())
        .testClasses(List.of(ClassDirs.test()))
        .build()) {
      assertThat(names(context.all())).contains(QuietLogging.class.getName(), LowercaseLogger.class.getName());
    }
  }

  @Test
  public void oneClassGroupWithoutTheOtherCannotRun() {
    try (ArchitectureContext context = ArchitectureContext.builder(PACKAGES)
        .mainClasses(List.of(ClassDirs.main()))
        .build()) {
      assertThatThrownBy(context::all)
          .as("ALL would silently miss the test classes")
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining(ArchitectureContext.TEST_CLASSES_PROPERTY);
      assertThatThrownBy(context::scan).isInstanceOf(ArchitectureRunnerError.class);
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
  public void scanExposesConstantsTheCompilerInlines() {
    try (ArchitectureContext context = new ArchitectureContext(List.of("dev.quokkify.architecture.fixtures.services"))) {
      assertThat(context.scan().getClassInfo(PoliteGreeting.class.getName()).getFieldInfo("TEXT")
          .getConstantInitializerValue())
          .isEqualTo(PoliteGreeting.TEXT);
    }
  }

  @Test
  public void resourcesAreReadFromTheConfiguredDirectoriesOnly() {
    try (ArchitectureContext context = ArchitectureContext.builder(PACKAGES)
        .mainResources(List.of(Path.of("src/test/resources/services/registered")))
        .testResources(List.of(Path.of("src/test/resources/services/absent")))
        .build()) {
      assertThat(context.resources().getPaths())
          .as("the log4j2-test.xml on the test classpath lies outside the configured directories")
          .containsExactly("META-INF/services/dev.quokkify.architecture.fixtures.services.Greeting");
    }
  }

  @Test
  public void classFilesInAResourceDirectoryAreNoResources() throws IOException {
    Path resources = Files.createTempDirectory("resources");
    Path compiled = ClassDirs.test().resolve(PoliteGreeting.class.getName().replace('.', '/') + ".class");
    Files.copy(compiled, resources.resolve("PoliteGreeting.class"));
    Files.writeString(resources.resolve("application.properties"), "key=value");
    try (ArchitectureContext context = ArchitectureContext.builder(PACKAGES)
        .mainResources(List.of(resources))
        .testResources(List.of())
        .build()) {
      assertThat(context.resources().getPaths()).containsExactly("application.properties");
    }
  }

  @Test
  public void oneResourceGroupWithoutTheOtherCannotRun() {
    try (ArchitectureContext context = ArchitectureContext.builder(PACKAGES)
        .mainResources(List.of())
        .build()) {
      assertThatThrownBy(context::resources)
          .as("a resource rule would silently miss the test resources")
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining(ArchitectureContext.TEST_RESOURCES_PROPERTY);
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
    assertThatThrownBy(context::resources).isInstanceOf(IllegalStateException.class);
  }

  private static List<String> names(Iterable<JavaClass> classes) {
    return java.util.stream.StreamSupport.stream(classes.spliterator(), false).map(JavaClass::getName).toList();
  }
}
