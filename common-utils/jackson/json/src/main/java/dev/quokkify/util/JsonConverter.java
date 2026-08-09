package dev.quokkify.util;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Utilities for JSON serialization and deserialization using Jackson.
 *
 * <p>Notes:
 * <ul>
 *   <li>No checked exceptions are thrown; they are wrapped into {@link RuntimeException}.</li>
 *   <li>Configured with sane date defaults and classpath module auto-discovery.</li>
 * </ul>
 */
public final class JsonConverter {

  private static final ObjectMapper JSON = createJsonMapper(false, true);
  private static final ObjectMapper JSON_NON_NULL = createJsonMapper(true, true);

  private JsonConverter() {
  }

  static ObjectMapper createJsonMapper(boolean ignoreNullFields, boolean registerDiscoveredModules) {
    JsonMapper.Builder builder = JsonMapper.builder()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    if (!ignoreNullFields) {
      builder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    } else {
      builder.defaultPropertyInclusion(
          JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS));
    }

    ObjectMapper mapper = builder.build();
    if (registerDiscoveredModules) {
      mapper.findAndRegisterModules();
    }
    return mapper;
  }

  /**
   * =========================
   * Read
   * =========================
   */

  public static <T> T fromObject(Object obj, Class<T> clazz) {
    try {
      return JSON.readValue(toJson(obj), clazz);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize from JSON (Object->" + clazz.getSimpleName() + ")", e);
    }
  }

  public static <T> T fromObject(Object obj, TypeReference<T> typeRef) {
    try {
      return JSON.readValue(toJson(obj), typeRef);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize from JSON (Object->TypeReference)", e);
    }
  }

  public static <T> T fromString(String json, Class<T> clazz) {
    try {
      return JSON.readValue(json, clazz);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize from JSON (String->" + clazz.getSimpleName() + ")", e);
    }
  }

  public static <T> T fromString(String json, TypeReference<T> typeRef) {
    try {
      return JSON.readValue(json, typeRef);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize from JSON (String->TypeReference)", e);
    }
  }

  public static <T> T fromFile(File jsonFile, Class<T> clazz) {
    try {
      return JSON.readValue(jsonFile, clazz);
    } catch (Exception e) {
      throw new RuntimeException("Failed to read JSON file: " + jsonFile, e);
    }
  }

  public static <T> T fromFile(File jsonFile, TypeReference<T> typeRef) {
    try {
      return JSON.readValue(jsonFile, typeRef);
    } catch (Exception e) {
      throw new RuntimeException("Failed to read JSON file (TypeReference): " + jsonFile, e);
    }
  }

  /**
   * Deserialize JSON with a parametric type, e.g., Response&lt;Item&gt;.
   */
  public static <T> T fromStringParametric(String json, Class<T> outerClass, Class<?> paramClass) {
    try {
      JavaType javaType = JSON.getTypeFactory().constructParametricType(outerClass, paramClass);
      return JSON.readValue(json, javaType);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize JSON with parametric type", e);
    }
  }

  /**
   * =========================
   * Write
   * =========================
   */

  public static String toJson(Object obj) {
    try {
      return JSON.writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize to JSON", e);
    }
  }

  public static String toJsonIgnoreNulls(Object obj) {
    try {
      return JSON_NON_NULL.writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize to JSON (ignore nulls)", e);
    }
  }
}
