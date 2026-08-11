package dev.quokkify.parser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import dev.quokkify.converter.XmlConverter;

/**
 * XML parser that delegates deserialization to {@link XmlConverter} (Jackson XmlMapper).
 *
 * <p>Notes:
 * <ul>
 *   <li>Does NOT close caller-owned streams.</li>
 *   <li>Provides convenient classpath resource loader.</li>
 * </ul>
 */
public final class XmlParser {

  private XmlParser() {
  }

  /**
   * Parse an XML stream into the specified entity type using XmlConverter.
   *
   * @param inputStream XML input stream (caller-owned; this method will NOT close it)
   * @param entityClass target entity class
   * @param <T>         type parameter
   * @return unmarshalled entity instance
   * @throws RuntimeException if parsing fails
   */
  public static <T> T parse(InputStream inputStream, Class<T> entityClass) {
    Objects.requireNonNull(inputStream, "inputStream");
    Objects.requireNonNull(entityClass, "entityClass");
    try {
      String xml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      return XmlConverter.fromString(xml, entityClass);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse XML into: " + entityClass.getName(), e);
    }
  }

  /**
   * Parse an XML classpath resource into the specified entity type using XmlConverter.
   *
   * @param classpathResource resource path on classpath (e.g., "test_file.xml")
   * @param entityClass       target entity class
   * @param <T>               type parameter
   * @return unmarshalled entity instance
   * @throws RuntimeException if parsing fails or resource is missing
   */
  public static <T> T parse(String classpathResource, Class<T> entityClass) {
    Objects.requireNonNull(classpathResource, "classpathResource");
    Objects.requireNonNull(entityClass, "entityClass");
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) cl = XmlParser.class.getClassLoader();
    try (InputStream is = cl.getResourceAsStream(classpathResource)) {
      if (is == null) {
        throw new IllegalArgumentException("Resource not found on classpath: " + classpathResource);
      }
      return parse(is, entityClass);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to parse XML resource '" + classpathResource + "' into: " + entityClass.getName(), e);
    }
  }
}
