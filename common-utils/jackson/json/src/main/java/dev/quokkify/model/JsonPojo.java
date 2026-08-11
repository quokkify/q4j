package dev.quokkify.model;

import java.util.Objects;
import java.util.Optional;

import dev.quokkify.util.JsonConverter;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Schema-less pojo backed by Jackson {@link ObjectNode}.
 *
 * <p>Designed for building request payloads piece by piece and for reading response payloads
 * in verification steps. All tree navigation and mutation is delegated to Jackson via
 * <a href="https://datatracker.ietf.org/doc/html/rfc6901">JSON Pointer</a> expressions,
 * e.g. {@code "/bonus/freespins_max_win"} or {@code "/inputs/0/value"}.
 *
 * @param json underlying json object as {@link ObjectNode}
 */
public record JsonPojo(ObjectNode json) implements Pojo {

  public JsonPojo() {
    this(JsonNodeFactory.instance.objectNode());
  }

  public JsonPojo(String json) {
    this(JsonConverter.fromString(json, ObjectNode.class));
  }

  /**
   * Create {@link JsonPojo} from any typed {@link Pojo}.
   *
   * @param pojo source pojo as {@link Pojo}
   * @return new {@link JsonPojo} with json tree of the source pojo
   */
  public static JsonPojo fromPojo(Pojo pojo) {
    return new JsonPojo(JsonConverter.fromObject(pojo, ObjectNode.class));
  }

  /**
   * Serialize the given pojos as a single json array.
   *
   * @param items pojos as {@link JsonPojo} varargs
   * @return json array as {@link String}
   */
  public static String asJsonArray(JsonPojo... items) {
    ArrayNode array = JsonNodeFactory.instance.arrayNode();
    for (JsonPojo item : items) {
      array.add(item.json);
    }
    return JsonConverter.toJson(array);
  }

  /**
   * Underlying json tree, also used as the serialized form of this record.
   *
   * @return underlying json as {@link ObjectNode}
   */
  @JsonValue
  public ObjectNode json() {
    return json;
  }

  /**
   * Serialize this pojo as a json array with a single element.
   *
   * @return json array as {@link String}
   */
  public String asJsonArray() {
    return asJsonArray(this);
  }

  /**
   * Set top-level field to any value (pojo, map, scalar, {@link JsonNode} or {@code null}).
   * {@link JsonNode} values are deep-copied, so later mutations do not leak between pojos.
   *
   * @param field top-level field name as {@link String}
   * @param value new field value as {@link Object}
   * @return self reference
   */
  public JsonPojo setField(String field, Object value) {
    json.set(field, toNode(value));
    return this;
  }

  /**
   * Set value at json pointer, creating missing intermediate objects/arrays via Jackson
   * {@link JsonNode#withObject(JsonPointer)}/{@link JsonNode#withArray(JsonPointer)}.
   *
   * <p>Container type is resolved from the existing node at the parent path. When the parent path
   * is missing, a numeric last segment creates an array (standard json pointer semantics),
   * so an object with a purely numeric key must exist before it can be addressed.
   *
   * @param jsonPointer json pointer expression as {@link String}, e.g. {@code "/bonus/freespins_max_win"}
   * @param value       new value as {@link Object}
   * @return self reference
   */
  public JsonPojo setAt(String jsonPointer, Object value) {
    setValueAt(json, JsonPointer.compile(jsonPointer), toNode(value));
    return this;
  }

  /**
   * Set value at relative json pointer inside each object element of the array at json pointer.
   *
   * <p>Non-object elements (scalars, nulls, nested arrays) are silently skipped. The operation
   * is not atomic: if setting fails on some element, previously processed elements stay mutated.
   *
   * @param arrayPointer   json pointer to array as {@link String}, e.g. {@code "/inputs"}
   * @param elementPointer json pointer relative to each array element as {@link String}, e.g. {@code "/value/games"}
   * @param value          new value as {@link Object}
   * @return self reference
   */
  public JsonPojo setEachAt(String arrayPointer, String elementPointer, Object value) {
    JsonNode arrayNode = json.requiredAt(arrayPointer);
    if (!arrayNode.isArray()) {
      throw new IllegalArgumentException("Node at '%s' is not an array".formatted(arrayPointer));
    }
    JsonPointer pointer = JsonPointer.compile(elementPointer);
    JsonNode valueNode = toNode(value);
    for (JsonNode element : arrayNode) {
      if (element instanceof ObjectNode objectNode) {
        setValueAt(objectNode, pointer, valueNode.deepCopy());
      }
    }
    return this;
  }

  /**
   * Remove node at json pointer. Missing path is a no-op.
   *
   * @param jsonPointer json pointer expression as {@link String}
   * @return self reference
   */
  public JsonPojo removeAt(String jsonPointer) {
    JsonPointer pointer = JsonPointer.compile(jsonPointer);
    JsonNode parent = json.at(pointer.head());
    JsonPointer leaf = pointer.last();
    if (parent instanceof ObjectNode objectNode) {
      objectNode.remove(leaf.getMatchingProperty());
    } else if (parent instanceof ArrayNode arrayNode && leaf.mayMatchElement()) {
      arrayNode.remove(leaf.getMatchingIndex());
    }
    return this;
  }

  /**
   * Get node at json pointer, see {@link JsonNode#at(String)}.
   *
   * @param jsonPointer json pointer expression as {@link String}
   * @return node as {@link JsonNode}, missing node if path is absent
   */
  public JsonNode at(String jsonPointer) {
    return json.at(jsonPointer);
  }

  /**
   * Get node at json pointer or fail, see {@link JsonNode#requiredAt(String)}.
   *
   * @param jsonPointer json pointer expression as {@link String}
   * @return node as {@link JsonNode}
   * @throws IllegalArgumentException if path is absent
   */
  public JsonNode requiredAt(String jsonPointer) {
    return json.requiredAt(jsonPointer);
  }

  /**
   * Read node at json pointer as typed value.
   *
   * @param jsonPointer json pointer expression as {@link String}
   * @param typeOfT     target type as {@link Class}
   * @param <T>         like {@link Object}
   * @return deserialized value of type T
   * @throws IllegalArgumentException if path is absent
   */
  public <T> T readAt(String jsonPointer, Class<T> typeOfT) {
    return JsonConverter.fromObject(json.requiredAt(jsonPointer), typeOfT);
  }

  /**
   * Find first object in the whole tree with field matching expected value.
   *
   * @param fieldName     field name as {@link String}
   * @param expectedValue expected field value as {@link String}
   * @return first matching object as {@link Optional} of {@link ObjectNode}
   */
  public Optional<ObjectNode> findFirstObjectByFieldValue(String fieldName, String expectedValue) {
    return json.findParents(fieldName).stream()
        .filter(ObjectNode.class::isInstance)
        .map(ObjectNode.class::cast)
        .filter(node -> Objects.equals(expectedValue, node.path(fieldName).asText()))
        .findFirst();
  }

  @Override
  public String asJson() {
    return JsonConverter.toJson(json);
  }

  private static void setValueAt(ObjectNode root, JsonPointer pointer, JsonNode value) {
    if (pointer.matches()) {
      throw new IllegalArgumentException("Json pointer must not point at the root node");
    }
    JsonPointer leaf = pointer.last();
    JsonNode parent = root.at(pointer.head());
    if (parent instanceof ObjectNode objectNode) {
      objectNode.set(leaf.getMatchingProperty(), value);
    } else if (parent instanceof ArrayNode arrayNode) {
      if (!leaf.mayMatchElement()) {
        throw new IllegalArgumentException(
            "Cannot set property '%s' on array node at '%s'".formatted(leaf.getMatchingProperty(), pointer.head()));
      }
      setInArray(arrayNode, leaf.getMatchingIndex(), value);
    } else if (leaf.mayMatchElement()) {
      setInArray(root.withArray(pointer.head()), leaf.getMatchingIndex(), value);
    } else {
      root.withObject(pointer.head()).set(leaf.getMatchingProperty(), value);
    }
  }

  private static void setInArray(ArrayNode array, int index, JsonNode value) {
    while (array.size() < index) {
      array.addNull();
    }
    if (array.size() == index) {
      array.add(value);
    } else {
      array.set(index, value);
    }
  }

  private static JsonNode toNode(Object value) {
    return value instanceof JsonNode node ? node.deepCopy() : JsonConverter.fromObject(value, JsonNode.class);
  }
}
