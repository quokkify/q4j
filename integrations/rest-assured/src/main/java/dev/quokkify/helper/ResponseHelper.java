package dev.quokkify.helper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import dev.quokkify.model.JsonPojo;
import dev.quokkify.model.JsonValidation;
import dev.quokkify.parser.RegexParser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.reinert.jjschema.v1.JsonSchemaV4Factory;
import io.restassured.http.Header;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.path.xml.exception.XmlPathException;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ResponseBodyExtractionOptions;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * Helper to work with Rest Assured {@link ValidatableResponse}.
 */
public final class ResponseHelper {

  private static final String BODY_TEXT_REGEX = "(?s)<body>(.*)</body>";
  private static final ObjectMapper JSON = new ObjectMapper();

  private ResponseHelper() {
  }

  /**
   * Create {@link JsonPojo} from response body.
   *
   * @param response {@link ValidatableResponse}
   * @return response body as {@link JsonPojo}
   */
  public static JsonPojo toJsonPojo(ValidatableResponse response) {
    return new JsonPojo(response.extract().asString());
  }

  /**
   * Get 'message' field value from response body (предпочтительно JSON).
   *
   * @param response {@link ValidatableResponse}
   * @return message text as {@link String}
   */
  public static String getMessageFromResponseBody(ValidatableResponse response) {
    if (isJson(response)) {
      try {
        String v = extractBody(response).jsonPath().getString("message");
        if (v != null) return v;
      } catch (JsonPathException ignore) {
      }
    }
    JsonNode root = readJsonNodeLenient(response);
    JsonNode node = safeGet(root, "message");
    if (node != null && !node.isNull()) {
      return node.asText();
    }
    throw new IllegalStateException("Cannot extract 'message' from response body");
  }

  /**
   * Get 'code' field value from response body (предпочтительно JSON).
   *
   * @param response {@link ValidatableResponse}
   * @return message text as {@link Integer}
   */
  public static int getCodeFromResponseBody(ValidatableResponse response) {
    if (isJson(response)) {
      try {
        Integer v = extractBody(response).jsonPath().get("code");
        if (v != null) return v;
      } catch (JsonPathException ignore) {
        // fallback ниже
      }
    }
    JsonNode root = readJsonNodeLenient(response);
    JsonNode node = safeGet(root, "code");
    if (node != null && node.isInt()) {
      return node.asInt();
    }
    if (node != null && node.isTextual()) {
      try {
        return Integer.parseInt(node.asText().trim());
      } catch (NumberFormatException ignore) {
        // упадём ниже с понятной ошибкой
      }
    }
    throw new IllegalStateException("Cannot extract 'code' from response body");
  }

  /**
   * Html tags will be removed from the result (на самом деле — пытаемся достать содержимое body).
   * Если htmlPath недоступен — возвращаем сырое тело.
   */
  public static String getBodyFromResponseBody(ValidatableResponse response) {
    try {
      Object bodyNode = extract(response).htmlPath().get("html.body");
      return bodyNode == null ? extractBodyAsString(response) : bodyNode.toString();
    } catch (Exception e) {
      // если это не HTML — вернём сырой текст
      return extractBodyAsString(response);
    }
  }

  /**
   * Get text including html tags, e.g. 'br', 'p', 'a'.
   */
  public static String getTextFromResponseBody(ValidatableResponse response) {
    String responseAsPrettyString = extract(response).asPrettyString();
    if (RegexParser.isMatched(BODY_TEXT_REGEX, responseAsPrettyString)) {
      return RegexParser.parse(BODY_TEXT_REGEX, responseAsPrettyString, 1);
    }
    return responseAsPrettyString;
  }

  public static String extractBodyAsString(ValidatableResponse response) {
    return extractBody(response).asString();
  }

  public static String extractBodyAsPrettyString(ValidatableResponse response) {
    return extractBody(response).asPrettyString();
  }

  public static Map<String, String> getCookies(ValidatableResponse response) {
    return extract(response).cookies();
  }

  public static String getCookie(ValidatableResponse response, String cookieName) {
    return extract(response).cookie(cookieName);
  }

  public static Header getHeader(ValidatableResponse response, String header) {
    return extract(response).headers().get(header);
  }

  public static int getStatusCode(ValidatableResponse response) {
    return extract(response).statusCode();
  }

  public static <T> ValidatableResponse validateSchema(ValidatableResponse response, Class<T> schemaSourceClass) {
    String schema = new JsonSchemaV4Factory().createSchema(schemaSourceClass).toPrettyString();
    return response.body(JsonSchemaValidator.matchesJsonSchema(schema));
  }

  public static ValidatableResponse validateSchema(ValidatableResponse response, JsonValidation jsonSchema) {
    return validateSchema(response, jsonSchema.getSchemaPath());
  }

  public static ValidatableResponse validateSchema(ValidatableResponse response, String schemaPath) {
    return response.body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaPath));
  }

  /* =========================================================
     ДОПОЛНИТЕЛЬНЫЕ УНИВЕРСАЛЬНЫЕ УТИЛИТЫ (НОВЫЕ)
     ========================================================= */

  public static Optional<JsonNode> getJsonField(ValidatableResponse response, String fieldName) {
    try {
      if (StringUtils.isBlank(fieldName)) return Optional.empty();
      JsonNode root = readJsonNodeLenient(response);
      JsonNode node = safeGet(root, fieldName);
      return Optional.ofNullable(node);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static Optional<JsonNode> tryExtractJsonFromHtmlBody(ValidatableResponse response) {
    String body = getBodyFromResponseBody(response);
    try {
      return Optional.of(JSON.readTree(body));
    } catch (JsonProcessingException e) {
      return Optional.empty();
    }
  }

  private static boolean isJson(ValidatableResponse response) {
    String ct = contentType(response);
    return ct.contains("application/json") || ct.contains("text/json") || ct.contains("+json");
  }

  private static boolean isXml(ValidatableResponse response) {
    String ct = contentType(response);
    return ct.contains("application/xml") || ct.contains("text/xml") || ct.contains("+xml");
  }

  private static String contentType(ValidatableResponse response) {
    // Берём header без падения на null
    try {
      String v = extract(response).contentType();
      return v == null ? "" : v.toLowerCase();
    } catch (Exception e) {
      return "";
    }
  }

  private static JsonNode readJsonNodeLenient(ValidatableResponse response) {
    String raw = extractBodyAsString(response);
    if (isJson(response)) {
      return readJson(raw);
    }
    if (isXml(response)) {
      try {
        extractBody(response).xmlPath().get();
      } catch (XmlPathException ignore) {
      }
    }
    String bodyOnly = tryExtractHtmlBody(raw).orElse(raw);
    if (isJson(bodyOnly)) {
      return readJson(bodyOnly);
    }

    int i = bodyOnly.indexOf('{');
    int j = bodyOnly.lastIndexOf('}');
    if (i >= 0 && j > i) {
      String candidate = bodyOnly.substring(i, j + 1);
      if (isJson(candidate)) {
        return readJson(candidate);
      }
    }

    throw new IllegalStateException("Response does not contain valid JSON");
  }

  private static boolean isJson(String s) {
    if (s == null) return false;
    String t = s.trim();
    return t.startsWith("{") && t.endsWith("}") || t.startsWith("[") && t.endsWith("]");
  }

  private static Optional<String> tryExtractHtmlBody(String raw) {
    if (raw == null) return Optional.empty();
    if (RegexParser.isMatched(BODY_TEXT_REGEX, raw)) {
      return Optional.ofNullable(RegexParser.parse(BODY_TEXT_REGEX, raw, 1));
    }
    return Optional.empty();
  }

  private static JsonNode readJson(String json) {
    try {
      byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
      return JSON.readTree(bytes);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse JSON", e);
    }
  }

  private static JsonNode safeGet(JsonNode node, String field) {
    if (node == null || StringUtils.isBlank(field)) return null;
    return node.get(field);
  }

  private static ResponseBodyExtractionOptions extractBody(ValidatableResponse response) {
    return extract(response).body();
  }

  private static ExtractableResponse<Response> extract(ValidatableResponse response) {
    return response.extract();
  }
}
