package dev.quokkify.architecture;

import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * Locates the class directories this module is compiled into, so tests can configure an
 * {@code ArchitectureContext} the way the build does.
 */
public final class ClassDirs {

  private ClassDirs() {
  }

  /**
   * Returns the directory the main classes of this module are compiled into.
   *
   * @return main class directory
   */
  public static Path main() {
    return locationOf(ArchitectureRunner.class);
  }

  /**
   * Returns the directory the test classes of this module are compiled into.
   *
   * @return test class directory
   */
  public static Path test() {
    return locationOf(ClassDirs.class);
  }

  private static Path locationOf(Class<?> type) {
    try {
      return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (URISyntaxException invalid) {
      throw new IllegalStateException(invalid);
    }
  }
}
