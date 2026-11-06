package dev.quokkify.arch;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import dev.quokkify.archgate.api.ProjectLayout;
import dev.quokkify.archgate.api.Scope;
import dev.quokkify.archgate.context.archunit.ArchUnitContext;
import dev.quokkify.archgate.context.archunit.ArchUnitContexts;

/**
 * A project layout over the fixtures of this module's tests.
 */
record FixtureLayout(String rootPackage, List<Path> mainClassDirs, List<Path> testClassDirs,
                     List<Path> mainSourceDirs, List<Path> testSourceDirs) implements ProjectLayout {

  /**
   * Returns an ArchUnit context whose test view holds the compiled fixtures under {@code fixturePackage}.
   */
  static ArchUnitContext classes(String fixturePackage) {
    return ArchUnitContexts.of(new FixtureLayout(fixturePackage, List.of(), List.of(testClassesDir()), List.of(),
        List.of()));
  }

  /**
   * Returns a layout whose test sources are the given directories.
   */
  static FixtureLayout testSources(String rootPackage, List<Path> sourceDirs) {
    return new FixtureLayout(rootPackage, List.of(), List.of(), List.of(), sourceDirs);
  }

  @Override
  public List<Path> classDirs(Scope scope) {
    return scope == Scope.MAIN ? mainClassDirs : testClassDirs;
  }

  @Override
  public List<Path> sourceDirs(Scope scope) {
    return scope == Scope.MAIN ? mainSourceDirs : testSourceDirs;
  }

  @Override
  public ClassLoader classLoader() {
    return FixtureLayout.class.getClassLoader();
  }

  private static Path testClassesDir() {
    try {
      return Path.of(FixtureLayout.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (URISyntaxException invalid) {
      throw new IllegalStateException("Cannot locate the test class directory", invalid);
    }
  }
}
