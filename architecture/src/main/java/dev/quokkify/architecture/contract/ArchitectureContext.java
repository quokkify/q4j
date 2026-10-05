package dev.quokkify.architecture.contract;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
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
 * <p>Three models are offered. The ArchUnit {@link JavaClasses} of {@link #all()}, with its
 * {@link #mainClasses()} and {@link #testClasses()} views, and the ClassGraph {@link ScanResult} read bytecode;
 * {@link #mainSources()} and {@link #testSources()} hold JavaParser syntax trees for rules that verify the
 * content of classes and methods. Each is created lazily on first use and then cached, so a scan or a parse
 * happens at most once per verification run regardless of how many rules are registered. With class
 * directories configured, only those directories are verified; with sources configured as well, generated
 * classes are left out of the ArchUnit models.
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

  /**
   * Comma separated main class directories, for example {@code -Darchitecture.main.classes=build/classes/java/main}.
   */
  public static final String MAIN_CLASSES_PROPERTY = "architecture.main.classes";

  /**
   * Comma separated test class directories, for example {@code -Darchitecture.test.classes=build/classes/java/test}.
   */
  public static final String TEST_CLASSES_PROPERTY = "architecture.test.classes";

  private static final String PACKAGE_INFO = "package-info";

  private final List<String> packages;
  private final List<Path> mainClassDirs;
  private final List<Path> testClassDirs;
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
    this(builder(packages));
  }

  private ArchitectureContext(Builder builder) {
    this.packages = requirePackages(builder.packages);
    this.mainClassDirs = copyOrNull(builder.mainClasses);
    this.testClassDirs = copyOrNull(builder.testClasses);
    this.mainSources = new JavaSources(MAIN_SOURCES_PROPERTY, builder.mainSources, this.packages);
    this.testSources = new JavaSources(TEST_SOURCES_PROPERTY, builder.testSources, this.packages);
  }

  /**
   * Starts a context covering the given root packages; class directories and source roots are optional.
   *
   * @param packages root packages to verify, at least one
   * @return builder of the context
   */
  public static Builder builder(Collection<String> packages) {
    return new Builder(packages);
  }

  /**
   * Creates a context from {@link #PACKAGES_PROPERTY} and the class and source properties. A class or source
   * property that is absent leaves that group unconfigured.
   *
   * @return context covering the configured packages, classes and sources
   * @throws ArchitectureRunnerError when the package property is missing or names no package
   */
  public static ArchitectureContext fromSystemProperties() {
    return builder(parsePackages(System.getProperty(PACKAGES_PROPERTY)))
        .mainClasses(parseRoots(System.getProperty(MAIN_CLASSES_PROPERTY)))
        .testClasses(parseRoots(System.getProperty(TEST_CLASSES_PROPERTY)))
        .mainSources(parseRoots(System.getProperty(MAIN_SOURCES_PROPERTY)))
        .testSources(parseRoots(System.getProperty(TEST_SOURCES_PROPERTY)))
        .build();
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
   * <p>When class directories are configured, only those directories are imported, so neither the runner nor
   * any dependency on the classpath is verified. Otherwise the packages are imported from the classpath. When
   * both main and test sources are configured, classes without an authored Java source, generated code above
   * all, are left out. The ClassGraph {@link #scan()} is not filtered this way.
   *
   * @return all classes of the covered packages
   */
  public JavaClasses all() {
    requireOpen();
    synchronized (javaClassesLock) {
      if (Objects.isNull(javaClasses)) {
        javaClasses = importClasses();
      }
      return javaClasses;
    }
  }

  /**
   * Returns the configured main class directories.
   *
   * @return immutable list of directories, empty when the project has no main classes
   * @throws ArchitectureRunnerError when no main class directory was configured
   */
  public List<Path> mainClassDirs() {
    requireOpen();
    return requireClassDirs(mainClassDirs, MAIN_CLASSES_PROPERTY);
  }

  /**
   * Returns the configured test class directories.
   *
   * @return immutable list of directories, empty when the project has no test classes
   * @throws ArchitectureRunnerError when no test class directory was configured
   */
  public List<Path> testClassDirs() {
    requireOpen();
    return requireClassDirs(testClassDirs, TEST_CLASSES_PROPERTY);
  }

  /**
   * Returns the classes compiled from the main source set, a view of {@link #all()}.
   *
   * @return main classes of the covered packages
   * @throws ArchitectureRunnerError when no main class directory was configured
   */
  public JavaClasses mainClasses() {
    return all().that(locatedIn(mainClassDirs()));
  }

  /**
   * Returns the classes compiled from the test source sets, a view of {@link #all()}.
   *
   * @return test classes of the covered packages
   * @throws ArchitectureRunnerError when no test class directory was configured
   */
  public JavaClasses testClasses() {
    return all().that(locatedIn(testClassDirs()));
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
        requireBothOrNoClassDirs();
        ClassGraph classGraph = new ClassGraph();
        if (hasClassDirs()) {
          classGraph.overrideClasspath(existingClassDirs());
        }
        scanResult = classGraph
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

  private JavaClasses importClasses() {
    requireBothOrNoClassDirs();
    JavaClasses imported = hasClassDirs()
        ? new ClassFileImporter()
            .importPaths(existingClassDirs())
            .that(JavaClass.Predicates.resideInAnyPackage(
                packages.stream().map(name -> name + "..").toArray(String[]::new)))
        : new ClassFileImporter().importPackages(packages);
    if (!mainSources.isConfigured() || !testSources.isConfigured()) {
      return imported;
    }
    requireJavaOnly(imported);
    return imported.that(declaredIn(authoredTypeNames()));
  }

  /**
   * Only Java sources are parsed, so a class compiled from Kotlin, Groovy or Scala would look generated and be
   * dropped. Such a class aborts the run instead of disappearing from every bytecode rule.
   */
  private static void requireJavaOnly(JavaClasses imported) {
    List<String> foreign = imported.stream()
        .filter(javaClass -> javaClass.getSource()
            .flatMap(source -> source.getFileName())
            .filter(fileName -> !fileName.endsWith(".java"))
            .isPresent())
        .map(JavaClass::getName)
        .sorted()
        .toList();
    if (!foreign.isEmpty()) {
      throw new ArchitectureRunnerError("""
          %d class(es) were not compiled from Java, for example %s. Only Java sources are parsed, so these \
          classes cannot be told apart from generated code. Leave the source properties unset for this project.\
          """.formatted(foreign.size(), foreign.get(0)));
    }
  }

  /**
   * Generated code, such as QueryDSL or annotation processor output, is compiled into the same directories as
   * authored code but has no source file under the configured roots. Keeping only classes whose top level type
   * is declared in those sources removes it from every bytecode rule, without a per rule exclusion list.
   */
  private Set<String> authoredTypeNames() {
    Set<String> names = new HashSet<>(mainSources.declaredTypeNames());
    names.addAll(testSources.declaredTypeNames());
    return names;
  }

  private static DescribedPredicate<JavaClass> declaredIn(Set<String> authored) {
    return DescribedPredicate.describe("declared in an authored source", javaClass -> {
      JavaClass topLevel = javaClass;
      while (topLevel.getEnclosingClass().isPresent()) {
        topLevel = topLevel.getEnclosingClass().get();
      }
      return authored.contains(topLevel.getName()) || PACKAGE_INFO.equals(topLevel.getSimpleName());
    });
  }

  /**
   * Main classes without test classes, or the reverse, would leave {@link ClassScope#ALL} silently missing one
   * half of the project.
   */
  private void requireBothOrNoClassDirs() {
    if (Objects.isNull(mainClassDirs) != Objects.isNull(testClassDirs)) {
      throw new ArchitectureRunnerError("""
          Only one of -D%s and -D%s is set. Set both, an empty value meaning that the project has no such \
          classes, or neither to import the packages from the classpath.\
          """.formatted(MAIN_CLASSES_PROPERTY, TEST_CLASSES_PROPERTY));
    }
  }

  private boolean hasClassDirs() {
    return Objects.nonNull(mainClassDirs) || Objects.nonNull(testClassDirs);
  }

  private List<Path> classDirs() {
    return Stream.of(mainClassDirs, testClassDirs)
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .toList();
  }

  private List<Path> existingClassDirs() {
    List<Path> existing = classDirs().stream().filter(Files::isDirectory).toList();
    if (existing.isEmpty()) {
      throw new ArchitectureRunnerError("""
          None of the configured class directories %s exists, so there is no class to verify. Build the classes \
          first, or do not run the verification for a project without code.""".formatted(classDirs()));
    }
    return existing;
  }

  private static List<Path> requireClassDirs(List<Path> dirs, String property) {
    if (Objects.isNull(dirs)) {
      throw new ArchitectureRunnerError("""
          No class directory was configured through -D%s, so main and test classes cannot be told apart. Pass \
          the compiled class directories, for example -D%s=build/classes/java/main.""".formatted(property, property));
    }
    return dirs;
  }

  private static DescribedPredicate<JavaClass> locatedIn(List<Path> dirs) {
    List<Path> roots = dirs.stream().map(dir -> dir.toAbsolutePath().normalize()).toList();
    return DescribedPredicate.describe("located in " + roots, javaClass -> javaClass.getSource()
        .map(source -> Path.of(source.getUri()).toAbsolutePath().normalize())
        .filter(location -> roots.stream().anyMatch(location::startsWith))
        .isPresent());
  }

  private static List<Path> copyOrNull(List<Path> paths) {
    return Objects.isNull(paths) ? null : List.copyOf(paths);
  }

  private static List<String> requirePackages(Collection<String> packages) {
    if (Objects.isNull(packages) || packages.isEmpty()) {
      throw new ArchitectureRunnerError("""
          No package to verify was configured, so no contract could be verified at all. Pass the root \
          packages of the project, for example -D%s=com.example.""".formatted(PACKAGES_PROPERTY));
    }
    return List.copyOf(packages);
  }

  /**
   * Configures an {@link ArchitectureContext}. Every group is optional: {@code null} leaves it unconfigured, an
   * empty list states that the project has none.
   */
  public static final class Builder {

    private final Collection<String> packages;
    private List<Path> mainClasses;
    private List<Path> testClasses;
    private List<Path> mainSources;
    private List<Path> testSources;

    private Builder(Collection<String> packages) {
      this.packages = packages;
    }

    /**
     * Sets the directories the main classes are compiled into.
     *
     * @param dirs class directories, empty when there are none, {@code null} when not configured
     * @return this builder
     */
    public Builder mainClasses(List<Path> dirs) {
      this.mainClasses = dirs;
      return this;
    }

    /**
     * Sets the directories the test classes are compiled into.
     *
     * @param dirs class directories, empty when there are none, {@code null} when not configured
     * @return this builder
     */
    public Builder testClasses(List<Path> dirs) {
      this.testClasses = dirs;
      return this;
    }

    /**
     * Sets the main source roots read by {@link ArchitectureContext#mainSources()}.
     *
     * @param roots source roots, empty when there are none, {@code null} when not configured
     * @return this builder
     */
    public Builder mainSources(List<Path> roots) {
      this.mainSources = roots;
      return this;
    }

    /**
     * Sets the test source roots read by {@link ArchitectureContext#testSources()}.
     *
     * @param roots source roots, empty when there are none, {@code null} when not configured
     * @return this builder
     */
    public Builder testSources(List<Path> roots) {
      this.testSources = roots;
      return this;
    }

    /**
     * Creates the context.
     *
     * @return the configured context
     * @throws ArchitectureRunnerError when no package is given
     */
    public ArchitectureContext build() {
      return new ArchitectureContext(this);
    }
  }

  private void requireOpen() {
    if (closed) {
      throw new IllegalStateException("""
          ArchitectureContext is closed. Build one context per verification run and use it only while the run is \
          in progress; reusing a closed context would repeat the full classpath scan.""");
    }
  }
}
