package dev.quokkify.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

/**
 * Utilities for XML serialization and deserialization using Jackson XmlMapper.
 *
 * <p>Notes:
 * <ul>
 *   <li>No checked exceptions are thrown; they are wrapped into {@link RuntimeException}.</li>
 *   <li>Configured with XML defaults and classpath module auto-discovery.</li>
 * </ul>
 */
public final class XmlConverter {

  private static final XmlMapper XML = createXmlMapper(false, true);
  private static final XmlMapper XML_NON_NULL = createXmlMapper(true, true);

  private XmlConverter() {
    // prevent instantiation
  }

  static XmlMapper createXmlMapper(boolean ignoreNullFields, boolean registerDiscoveredModules) {
    XmlMapper.Builder builder = XmlMapper.builder()
        .disable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    if (!ignoreNullFields) {
      builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    } else {
      builder.defaultPropertyInclusion(
          JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS));
    }

    XmlMapper mapper = builder.build();
    if (registerDiscoveredModules) {
      mapper.findAndRegisterModules();
    }
    return mapper;
  }

  /* =========================
     Read
     ========================= */

  public static <T> T fromObject(Object obj, Class<T> clazz) {
    try {
      return XML.readValue(toXml(obj), clazz);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize from XML (Object->" + clazz.getSimpleName() + ")", e);
    }
  }

  public static <T> T fromObject(Object obj, TypeReference<T> typeRef) {
    try {
      return XML.readValue(toXml(obj), typeRef);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize from XML (Object->TypeReference)", e);
    }
  }

  public static <T> T fromString(String xml, Class<T> clazz) {
    try {
      return XML.readValue(xml, clazz);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize from XML (String->" + clazz.getSimpleName() + ")", e);
    }
  }

  public static <T> T fromString(String xml, TypeReference<T> typeRef) {
    try {
      return XML.readValue(xml, typeRef);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize from XML (String->TypeReference)", e);
    }
  }

  /* =========================
     Write
     ========================= */

  public static String toXml(Object obj) {
    try {
      return XML.writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize to XML", e);
    }
  }

  public static String toXmlIgnoreNulls(Object obj) {
    try {
      return XML_NON_NULL.writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize to XML (ignore nulls)", e);
    }
  }
}
