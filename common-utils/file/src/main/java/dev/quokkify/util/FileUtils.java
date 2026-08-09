package dev.quokkify.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import dev.quokkify.constant.StringConstant;
import dev.quokkify.model.ConstantFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for working with file resources within the classpath.
 */
public final class FileUtils {

  private static final Logger LOG = LogManager.getLogger(FileUtils.class);
  private static final Object FILE_WRITE_LOCK = new Object();

  private FileUtils() {
  }

  /**
   * Read the full content of the file located at the given path.
   *
   * @param path path to the file to read; must point to an existing readable file
   * @return the file content as a {@link String}
   * @throws RuntimeException if an {@link IOException} occurs
   */
  public static String readAsString(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file: " + path, e);
    }
  }

  /**
   * Checks if a resource exists in the classpath.
   *
   * @param path the resource path
   * @return {@code true} if the resource exists, otherwise {@code false}
   */
  public static boolean isResourceExist(String path) {
    return Objects.nonNull(getResourceAsStream(path));
  }

  /**
   * Returns the file path of a resource in the classpath.
   *
   * @param path the resource path
   * @return the file path
   * @throws NullPointerException if the resource is not found
   */
  public static String getResourceAsFilePath(String path) {
    return Objects.requireNonNull(getResourceUrl(path), "Resource not found: " + path).getFile();
  }

  /**
   * Returns the content of a classpath resource as a {@link String}.
   *
   * @param path the resource path
   * @return the resource content as a string
   * @throws RuntimeException if the resource cannot be read
   */
  public static String getResourceAsString(String path) {
    try {
      return Files.readString(Paths.get(getResourcePath(path)));
    } catch (IOException e) {
      throw new RuntimeException("Failed to read resource as string: " + path, e);
    }
  }

  /**
   * Returns an {@link InputStream} for a classpath resource.
   *
   * @param path the resource path
   * @return {@link InputStream} or {@code null} if the resource does not exist
   */
  public static InputStream getResourceAsStream(String path) {
    return FileUtils.class.getClassLoader().getResourceAsStream(path);
  }

  /**
   * Returns the absolute path of a resource in the classpath.
   *
   * @param path the resource path
   * @return the absolute path of the resource
   * @throws IllegalArgumentException if the resource cannot be loaded or converted to a valid path
   */
  public static String getResourcePath(String path) {
    URL resourceUrl = getResourceUrl(path);
    if (resourceUrl != null) {
      try {
        return Paths.get(resourceUrl.toURI()).toAbsolutePath().toString();
      } catch (URISyntaxException e) {
        LOG.error("URI processing error: {}", e.getMessage(), e);
      }
    }
    throw new IllegalArgumentException("Cannot load '%s' file".formatted(path));
  }

  /**
   * Returns a list of paths for resources that match the given path and contain the specified module name.
   *
   * @param path   the path to the resource
   * @param module the module name to filter by
   * @return a list of resource paths
   * @throws RuntimeException if resources cannot be enumerated
   */
  public static List<String> getResourcePath(String path, String module) {
    try {
      return Collections.list(FileUtils.class.getClassLoader().getResources(path))
          .stream()
          .map(URL::getPath)
          .filter(resourcePath -> resourcePath.contains(module))
          .toList();
    } catch (IOException e) {
      throw new RuntimeException("Failed to get resource paths for: " + path, e);
    }
  }

  /**
   * Returns the {@link URL} of a classpath resource.
   *
   * @param path the resource path
   * @return the resource URL or {@code null} if not found
   */
  public static URL getResourceUrl(String path) {
    return FileUtils.class.getClassLoader().getResource(path);
  }

  /**
   * Returns a non-null {@link InputStream} for a classpath resource.
   *
   * @param path the resource path
   * @return non-null {@link InputStream}
   * @throws IllegalArgumentException if the resource is not found
   */
  public static InputStream getNonNullResourceAsStream(String path) {
    InputStream stream = FileUtils.class.getClassLoader().getResourceAsStream(path);
    if (stream == null) {
      throw new IllegalArgumentException("Cannot load '%s' file".formatted(path));
    }
    return stream;
  }

  /**
   * Removes the "file:/" prefix from a file path.
   *
   * @param path file path
   * @return the modified path without the prefix
   */
  public static String deletePathFilePrefix(String path) {
    Objects.requireNonNull(path, "path must not be null");
    return path.replace("file:/", StringConstant.BACK_SLASH);
  }

  /**
   * Appends text to a file, creating the file if it does not exist.
   *
   * @param fileName the file name
   * @param text     text to append
   * @throws RuntimeException if an {@link IOException} occurs
   */
  public static void addTextToFile(String fileName, String text) {
    synchronized (FILE_WRITE_LOCK) {
      try {
        Files.writeString(
            Paths.get(fileName),
            "%s%n".formatted(text),
            StandardOpenOption.APPEND,
            StandardOpenOption.CREATE
        );
      } catch (IOException e) {
        throw new RuntimeException("Failed to write text to file: " + fileName, e);
      }
    }
  }

  /**
   * Appends a list of texts to a file, creating the file if it does not exist.
   *
   * @param fileName the file name
   * @param texts    list of texts to append
   * @throws RuntimeException if an {@link IOException} occurs
   */
  public static void addTextsToFile(String fileName, List<String> texts) {
    synchronized (FILE_WRITE_LOCK) {
      try {
        Files.writeString(
            Paths.get(fileName),
            "[%s]%n".formatted(String.join(StringConstant.COMMA, texts)),
            StandardOpenOption.APPEND,
            StandardOpenOption.CREATE
        );
      } catch (IOException e) {
        throw new RuntimeException("Failed to write texts to file: " + fileName, e);
      }
    }
  }

  /**
   * Creates a temporary file with a random name.
   *
   * @param fileExtension file extension
   * @return the created temporary file
   * @throws RuntimeException if an {@link IOException} occurs
   */
  public static File createTempFile(FileExtension fileExtension) {
    return createTempFile(CommonRandomData.uuid().toString(), fileExtension);
  }

  /**
   * Creates a temporary file.
   *
   * @param fileName      file name
   * @param fileExtension file extension
   * @return the created temporary file
   * @throws RuntimeException if an {@link IOException} occurs
   */
  public static File createTempFile(String fileName, FileExtension fileExtension) {
    try {
      return File.createTempFile(fileName, fileExtension.getExtension());
    } catch (IOException e) {
      throw new RuntimeException("Failed to create temp file: " + fileName, e);
    }
  }

  /**
   * Compares file contents.
   *
   * @param firstFile  first file
   * @param secondFile second file
   * @return {@code true} if contents are equal, otherwise {@code false}
   * @throws RuntimeException if an {@link IOException} occurs
   */
  public static boolean isFilesContentEquals(File firstFile, File secondFile) {
    try {
      return org.apache.commons.io.FileUtils.contentEquals(firstFile, secondFile);
    } catch (IOException e) {
      throw new RuntimeException("Failed to compare files: %s and %s"
          .formatted(firstFile, secondFile), e);
    }
  }

  /**
   * Returns enum values corresponding to sub-directories found at the given configuration path.
   *
   * @param configurationPath path to the configuration directory
   * @param clazz             enum class to map directory names to
   * @param <T>               enum type
   * @return list of matched enum values; empty if the path does not exist
   * @throws RuntimeException if directory listing fails
   */
  public static <T extends Enum<T>> List<T> getDirectoriesAsEnumValuesFromConfiguration(
      String configurationPath, Class<T> clazz) {
    Path pathOfConfig = Path.of(configurationPath);
    if (Files.notExists(pathOfConfig)) {
      return Collections.emptyList();
    }
    try (var stream = Files.list(pathOfConfig)) {
      return stream
          .filter(Files::isDirectory)
          .map(Path::getFileName)
          .filter(Objects::nonNull)
          .map(Path::toString)
          .map(name -> {
            try {
              return Enum.valueOf(clazz,
                  name.replace(StringConstant.DASH, StringConstant.UNDERSCORE).toUpperCase());
            } catch (IllegalArgumentException e) {
              return null;
            }
          })
          .filter(Objects::nonNull)
          .toList();
    } catch (IOException e) {
      throw new RuntimeException("Failed to list directories at: " + configurationPath, e);
    }
  }

  public enum FileExtension implements ConstantFormat {
    CSV;

    public String getExtension() {
      return StringConstant.DOT.concat(this.lowerCase());
    }

    @Override
    public String formatValue() {
      return name();
    }
  }
}
