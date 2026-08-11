package dev.quokkify.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.quokkify.util.FileUtils;
import dev.quokkify.util.JsonConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Utilities for loading YAML resources from the filesystem or classpath and converting them into
 * Java objects, maps, or lists while preserving key order where applicable.
 *
 * <p>Example usage:</p>
 * <pre>
 *   MyClass obj = YamlParser.loadAsObjectFromResources("file.yaml", MyClass.class);
 *   Map&lt;String, MyClass&gt; map = YamlParser.loadAsMapFromResources("file.yaml", MyClass.class);
 *   List&lt;MyClass&gt; list = YamlParser.loadListFromResources("list.yaml", MyClass.class);
 * </pre>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>Creates a new {@link Yaml} instance per call (SnakeYAML is not thread-safe).</li>
 *   <li>Converts values using {@link JsonConverter#fromObject(Object, Class)} to the target type.</li>
 *   <li>Preserves insertion order of YAML mappings via {@link LinkedHashMap}.</li>
 * </ul>
 */
public final class YamlParser {

  private static final Logger LOG = LogManager.getLogger(YamlParser.class);

  private YamlParser() {
  }

  /* =========================
     Public API — typed loaders
     ========================= */

  /**
   * Load a YAML resource from the classpath and convert it to an object of the given class.
   * Use this for scalar or object roots (not lists).
   */
  public static <T> T loadAsObjectFromResources(String yamlResource, Class<T> type) {
    LOG.debug("Load YAML '{}' as '{}' object", yamlResource, type.getName());
    try (InputStream in = FileUtils.getNonNullResourceAsStream(yamlResource)) {
      Object raw = newYaml().load(in);
      return JsonConverter.fromObject(raw, type);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load YAML resource as object: " + yamlResource, e);
    }
  }

  /**
   * Load a YAML resource whose root is a sequence and convert to {@code List<T>}.
   */
  public static <T> List<T> loadListFromResources(String yamlResource, Class<T> elementType) {
    LOG.debug("Load YAML '{}' as 'List<{}>'", yamlResource, elementType.getName());
    try (InputStream in = FileUtils.getNonNullResourceAsStream(yamlResource)) {
      Object raw = newYaml().load(in);
      if (raw == null) return List.of();
      if (!(raw instanceof List<?> rawList)) {
        throw new RuntimeException("YAML root is not a sequence (list): " + yamlResource);
      }
      List<T> out = new ArrayList<>(rawList.size());
      for (Object item : rawList) {
        out.add(JsonConverter.fromObject(item, elementType));
      }
      return out;
    } catch (Exception e) {
      throw new RuntimeException("Failed to load YAML resource as list: " + yamlResource, e);
    }
  }

  /**
   * Load a YAML resource from the classpath and convert it to a {@code Map<String, T>}.
   * Key order is preserved.
   */
  public static <T> Map<String, T> loadAsMapFromResources(String yamlResource, Class<T> type) {
    LOG.debug("Load YAML '{}' as 'Map<String, {}>'", yamlResource, type.getName());
    try (InputStream in = FileUtils.getNonNullResourceAsStream(yamlResource)) {
      Object rawAny = newYaml().load(in);
      if (rawAny == null) return new LinkedHashMap<>();
      if (!(rawAny instanceof Map<?, ?> raw)) {
        throw new RuntimeException("YAML root is not a mapping: " + yamlResource);
      }
      if (raw.isEmpty()) return new LinkedHashMap<>();
      return castMapValues(raw, type);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load YAML resource as map: " + yamlResource, e);
    }
  }

  /**
   * Load a YAML resource as {@code Map<String, T>} and return its values as a list.
   * Order follows insertion order in YAML.
   */
  public static <T> List<T> loadValuesFromMapFromResources(String yamlResource, Class<T> type) {
    LOG.debug("Load YAML '{}' as '{}' values from map", yamlResource, type.getName());
    return new ArrayList<>(loadAsMapFromResources(yamlResource, type).values());
  }

  /* =========================
     Public API — raw loaders
     ========================= */

  public static <T> T load(File yamlFile) {
    LOG.debug("Load YAML file '{}'", yamlFile);
    try (InputStream in = new FileInputStream(yamlFile)) {
      return newYaml().load(in);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load YAML file: " + yamlFile, e);
    }
  }

  public static <T> T load(String yamlResource) {
    LOG.debug("Load YAML resource '{}'", yamlResource);
    try (InputStream in = FileUtils.getNonNullResourceAsStream(yamlResource)) {
      return newYaml().load(in);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load YAML resource: " + yamlResource, e);
    }
  }

  /* =========================
     Helpers
     ========================= */

  private static Yaml newYaml() {
    LoaderOptions options = new LoaderOptions();
    options.setAllowDuplicateKeys(false);
    options.setMaxAliasesForCollections(50);
    return new Yaml(new SafeConstructor(options));
  }

  private static <T> Map<String, T> castMapValues(Map<?, ?> loadedMap, Class<T> valueClass) {
    LOG.debug("Cast map values to '{}' type", valueClass.getName());
    return loadedMap.entrySet().stream()
        .collect(Collectors.toMap(
            e -> (String) e.getKey(),
            e -> JsonConverter.fromObject(e.getValue(), valueClass),
            (oldV, newV) -> oldV,
            LinkedHashMap::new
        ));
  }
}
