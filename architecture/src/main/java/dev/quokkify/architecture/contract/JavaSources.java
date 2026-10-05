package dev.quokkify.architecture.contract;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;

/**
 * Java source files of one group of source roots, parsed with JavaParser on first use.
 *
 * <p>Bytecode loses what a method body says: which calls it makes, which literals and comments it contains.
 * This model keeps the source syntax tree, so a rule can verify the content of classes and methods.
 *
 * <p>Only compilation units whose package lies under the packages of the owning {@link ArchitectureContext} are
 * returned, the same selection the classpath models use. Roots are optional: when none were configured, the
 * group is not configured and reading it fails with {@link ArchitectureRunnerError}, since a rule that reads
 * sources would otherwise pass without having read any. A configured but empty group is valid and means the
 * project has no such sources.
 *
 * <p>Parsing happens at most once, under a lock, and the returned syntax trees must only be read: rules run
 * concurrently, so they must not mutate a tree or call {@code Node.toString()}, which lazily stores a printer in
 * the shared compilation unit. A file that does not parse aborts the run, and so do sources of which none lies
 * under the verified packages: a rule cannot vouch for code it could not read.
 */
public final class JavaSources {

  private static final String JAVA_EXTENSION = ".java";
  private static final String MODULE_INFO = "module-info.java";

  private final String property;
  private final List<Path> roots;
  private final List<String> packages;
  private final Object unitsLock = new Object();

  private volatile List<CompilationUnit> units;

  JavaSources(String property, List<Path> roots, List<String> packages) {
    this.property = property;
    this.roots = Objects.isNull(roots) ? null : List.copyOf(roots);
    this.packages = packages;
  }

  /**
   * Tells whether source roots were configured for this group, possibly none.
   *
   * @return {@code true} when the group was configured
   */
  public boolean isConfigured() {
    return Objects.nonNull(roots);
  }

  /**
   * Returns the configured source roots.
   *
   * @return immutable list of roots, empty when the project has no such sources
   * @throws ArchitectureRunnerError when the group was not configured
   */
  public List<Path> roots() {
    if (!isConfigured()) {
      throw new ArchitectureRunnerError("""
          No source root was configured through -D%s, so a rule reading sources could not verify anything. Pass \
          the source directories, for example -D%s=src/main/java.""".formatted(property, property));
    }
    return roots;
  }

  /**
   * Returns the parsed compilation units under the packages of the context, parsing them on first call.
   *
   * @return immutable list of compilation units, ordered by file path
   * @throws ArchitectureRunnerError when the group was not configured, a root is missing or a file does not parse
   */
  public List<CompilationUnit> units() {
    List<Path> configuredRoots = roots();
    synchronized (unitsLock) {
      if (Objects.isNull(units)) {
        units = parse(configuredRoots);
      }
      return units;
    }
  }

  private List<CompilationUnit> parse(List<Path> configuredRoots) {
    JavaParser parser = new JavaParser(
        new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    List<CompilationUnit> parsed = new ArrayList<>();
    List<String> problems = new ArrayList<>();
    List<Path> files = javaFiles(configuredRoots);
    for (Path file : files) {
      ParseResult<CompilationUnit> result = parseFile(parser, file);
      if (result.isSuccessful() && result.getResult().isPresent()) {
        result.getResult().filter(this::isInPackages).ifPresent(parsed::add);
      } else {
        problems.add("%s: %s".formatted(file, result.getProblems()));
      }
    }
    if (!problems.isEmpty()) {
      throw new ArchitectureRunnerError("JavaParser could not parse %d file(s), so they cannot be verified:%n%s"
          .formatted(problems.size(), String.join(System.lineSeparator(), problems)));
    }
    if (parsed.isEmpty() && !files.isEmpty()) {
      // Sources exist but none lies under the verified packages: every source rule would pass vacuously.
      throw new ArchitectureRunnerError("""
          %d source file(s) under %s were found, but none declares a package under %s, so no source was \
          verified. Fix -D%s or the source roots.""".formatted(files.size(), configuredRoots, packages,
          ArchitectureContext.PACKAGES_PROPERTY));
    }
    return List.copyOf(parsed);
  }

  private static List<Path> javaFiles(List<Path> configuredRoots) {
    List<Path> files = new ArrayList<>();
    for (Path root : configuredRoots) {
      if (!Files.isDirectory(root)) {
        throw new ArchitectureRunnerError("Source root %s does not exist, so its sources cannot be verified."
            .formatted(root));
      }
      try (Stream<Path> walk = Files.walk(root)) {
        walk.filter(Files::isRegularFile)
            .filter(file -> file.toString().endsWith(JAVA_EXTENSION))
            .filter(file -> !file.endsWith(MODULE_INFO))
            .sorted()
            .forEach(files::add);
      } catch (IOException | UncheckedIOException unreadable) {
        throw new ArchitectureRunnerError("Cannot list the sources under %s.".formatted(root), unreadable);
      }
    }
    return files;
  }

  private static ParseResult<CompilationUnit> parseFile(JavaParser parser, Path file) {
    try {
      return parser.parse(file);
    } catch (IOException unreadable) {
      throw new ArchitectureRunnerError("Cannot read source file %s.".formatted(file), unreadable);
    }
  }

  private boolean isInPackages(CompilationUnit unit) {
    String packageName = unit.getPackageDeclaration().map(declaration -> declaration.getNameAsString()).orElse("");
    return packages.stream().anyMatch(root -> packageName.equals(root) || packageName.startsWith(root + "."));
  }
}
