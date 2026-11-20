package dev.quokkify.flutter.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.quokkify.util.JsonConverter;

import com.fasterxml.jackson.databind.JsonNode;

public final class WidgetTreeParser {

  private static final Pattern VALUE_KEY = Pattern.compile("^\\[<'?(.*?)'?>]$");
  private static final Rect EMPTY_RECT = new Rect(0, 0, 0, 0);

  private WidgetTreeParser() {
  }

  public static WidgetNode parse(String json) {
    return toNode(JsonConverter.fromString(json, JsonNode.class));
  }

  public static WidgetNode fromScriptResult(Object result) {
    if (result instanceof String json) {
      return parse(json);
    }
    if (result instanceof Map<?, ?> map) {
      return toNode(JsonConverter.fromObject(map, JsonNode.class));
    }
    if (result instanceof List<?> list && list.size() == 1) {
      return fromScriptResult(list.get(0));
    }
    throw new IllegalArgumentException("Unsupported widget tree script result: "
        + (result == null ? "null" : result.getClass().getName()));
  }

  public static String normalizeKey(String key) {
    if (key == null) {
      return null;
    }
    Matcher matcher = VALUE_KEY.matcher(key);
    return matcher.matches() ? matcher.group(1) : key;
  }

  private static WidgetNode toNode(JsonNode node) {
    JsonNode attributes = node.path("attributes");
    JsonNode state = node.path("state");
    List<WidgetNode> children = new ArrayList<>();
    node.path("children").forEach(child -> children.add(toNode(child)));
    return new WidgetNode(
        string(node, "type"),
        string(node, "elementType"),
        string(node, "description"),
        node.path("depth").asInt(0),
        normalizeKey(string(node, "key")),
        string(attributes, "text"),
        string(attributes, "semanticsLabel"),
        string(attributes, "tooltip"),
        string(attributes, "hintText"),
        state.path("visible").asBoolean(false),
        state.path("enabled").asBoolean(false),
        state.path("focused").asBoolean(false),
        rect(node.path("rect")),
        List.copyOf(children));
  }

  private static Rect rect(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) {
      return EMPTY_RECT;
    }
    return new Rect(node.path("x").asDouble(0), node.path("y").asDouble(0),
        node.path("width").asDouble(0), node.path("height").asDouble(0));
  }

  private static String string(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? null : value.asText();
  }
}
