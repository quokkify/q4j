package dev.quokkify.architecture.contract;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

/**
 * Shared classpath model built once and reused by every {@link ArchitectureRule}.
 *
 * <p>The model covers only the packages the context was created for, which is what keeps it independent of
 * any particular project: the consumer names its root packages through {@link #PACKAGES_PROPERTY} or the
 * constructor, and every rule reads the same selection.
 *
 * <p>Three models are offered: the ArchUnit {@link JavaClasses} and the ClassGraph {@link ScanResult} read
 * bytecode, while {@link #mainSources()} and {@link #testSources()} hold JavaParser syntax trees for rules that
 * verify the content of classes and methods. Each is created lazily on first use and then cached, so a scan or
 * a parse happens at most once per verification run regardless of how many rules are registered.
 *
 * <p>This class is safe to share between concurrently evaluated rules, which is how
 * {@code ArchitectureRunner} uses it. Each cache has its own lock, so a rule waiting for the ArchUnit
 * import does not block a rule that only needs the ClassGraph scan, and the two scans overlap instead of
 * running one after the other. A second caller of the same accessor waits for the in-flight scan and then
 * receives the cached model, so a full scan still happens at most once per run.
 *
 * <p>Only the initialisation is guarded. The models it produces are queried without a lock, which is sound
 * because a rule may only read them: neither the {@link JavaClasses} nor the {@link ScanResult} is mutated
 * after it has been built.
 */
public class ArchitectureContext implements AutoCloseable {

  /**
   * Comma separated root packages to verify, for example {@code -Darchitecture.packages=dev.quokkify}.
   */
  public static final String PACKAGES_PROPERTY = "architecture.packages";

  /**
   * Comma separated main source roots, for example {@code -Darchitecture.main.sources=src/main/java}.
   */
  public static final String MAIN_SOURCES_PROPERTY = "architecture.main.sources";

  /**
   * Comma separated test source roots, for example {@code -Darchitecture.test.sources=src/test/java}.
   */
  public static final String TEST_SOURCES_PROPERTY = "architecture.test.sources";

  private final List<String> packages;
  private final JavaSources mainSources;
  private final JavaSources testSources;

  private final Object javaClassesLock = new Object();
  private final Object scanResultLock = new Object();

  private volatile JavaClasses javaClasses;
  private volatile ScanResult scanResult;
  private volatile boolean closed;

  /**
   * Creates a context covering the given root packages and their subpackages.
   *
   * @param packages root packages to verify, at least one
   * @throws ArchitectureRunnerError when no package is given, since a rule could then verify nothing
   */
  public ArchitectureContext(Collection<String> packages) {
    this(packages, null, null);
  }

  /**
   * Creates a context covering the given root packages, with the source roots read by source based rules.
   *
   * @param packages        root packages to verify, at least one
   * @param mainSourceRoots main source roots, empty when there are none, {@code null} when not configured
   * @param testSourceRoots test source roots, empty when there are none, {@code null} when not configured
   * @throws ArchitectureRunnerError when no package is given, since a rule could then verify nothing
   */
  public ArchitectureContext(Collection<String> packages, List<Path> mainSourceRoots, List<Path> testSourceRoots) {
    this.packages = requirePackages(packages);
    this.mainSources = new JavaSources(MAIN_SOURCES_PROPERTY, mainSourceRoots, this.packages);
    this.testSources = new JavaSources(TEST_SOURCES_PROPERTY, testSourceRoots, this.packages);
  }

  /**
   * Creates a context from {@link #PACKAGES_PROPERTY}, {@link #MAIN_SOURCES_PROPERTY} and
   * {@link #TEST_SOURCES_PROPERTY}. A source property that is absent leaves that group unconfigured.
   *
   * @return context covering the configured packages and sources
   * @throws ArchitectureRunnerError when the package property is missing or names no package
   */
  public static ArchitectureContext fromSystemProperties() {
    return new ArchitectureContext(
        parsePackages(System.getProperty(PACKAGES_PROPERTY)),
        parseRoots(System.getProperty(MAIN_SOURCES_PROPERTY)),
        parseRoots(System.getProperty(TEST_SOURCES_PROPERTY)));
  }

  /**
   * Splits a comma separated list of source roots.
   *
   * @param configured comma separated directories, may be {@code null}
   * @return the roots, or {@code null} when the property is absent, which leaves the group unconfigured
   */
  static List<Path> parseRoots(String configured) {
    if (Objects.isNull(configured)) {
      return null;
    }
    return parsePackages(configured).stream().map(Path::of).toList();
  }

  /**
   * Splits a comma separated package list, kept separate from the system property so it can be tested
   * without touching global state.
   *
   * @param configured comma separated package names, may be {@code null}
   * @return the non blank, trimmed package names
   */
  static List<String> parsePackages(String configured) {
    if (Objects.isNull(configured)) {
      return List.of();
    }
    return Arrays.stream(configured.split(","))
        .map(String::trim)
        .filter(name -> !name.isEmpty())
        .toList();
  }

  /**
   * Returns the root packages this context covers.
   *
   * @return immutable, non empty list of package names
   */
  public List<String> packages() {
    return packages;
  }

  /**
   * Returns the main sources, parsed with JavaParser on first use.
   *
   * @return main source model
   */
  public JavaSources mainSources() {
    requireOpen();
    return mainSources;
  }

  /**
   * Returns the test sources, parsed with JavaParser on first use.
   *
   * @return test source model
   */
  public JavaSources testSources() {
    requireOpen();
    return testSources;
  }

  /**
   * Returns every class under {@link #packages()} as an ArchUnit model, importing it on first call.
   *
   * @return all classes of the covered packages
   */
  public JavaClasses all() {
    requireOpen();
    synchronized (javaClassesLock) {
      if (Objects.isNull(javaClasses)) {
        javaClasses = new ClassFileImporter().importPackages(packages);
      }
      return javaClasses;
    }
  }

  /**
   * Returns the ClassGraph scan used for annotation and method metadata queries.
   *
   * @return scan result covering {@link #packages()}
   */
  public ScanResult scan() {
    requireOpen();
    synchronized (scanResultLock) {
      if (Objects.isNull(scanResult)) {
        scanResult = new ClassGraph()
            .acceptPackages(packages.toArray(String[]::new))
            .enableClassInfo()
            .enableMethodInfo()
            .enableAnnotationInfo()
            .ignoreMethodVisibility()
            .scan();
      }
      return scanResult;
    }
  }

  /**
   * Releases the ClassGraph scan result and makes this context unusable.
   *
   * <p>Closing marks the context instead of clearing the cached scan, so a rule that runs after the
   * verification finished fails loudly rather than silently triggering a second full classpath scan whose
   * result nobody would close.
   */
  @Override
  public void close() {
    closed = true;
    synchronized (scanResultLock) {
      if (Objects.nonNull(scanResult)) {
        scanResult.close();
      }
    }
  }

  private static List<String> requirePackages(Collection<String> packages) {
    if (Objects.isNull(packages) || packages.isEmpty()) {
      throw new ArchitectureRunnerError("""
          No package to verify was configured, so no contract could be verified at all. Pass the root \
          packages of the project, for example -D%s=com.example.""".formatted(PACKAGES_PROPERTY));
    }
    return List.copyOf(packages);
  }

  private void requireOpen() {
    if (closed) {
      throw new IllegalStateException("""
          ArchitectureContext is closed. Build one context per verification run and use it only while the run is \
          in progress; reusing a closed context would repeat the full classpath scan.""");
    }
  }
}
