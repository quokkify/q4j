package dev.quokkify.flutter.locator;

import dev.quokkify.util.JsonConverter;

import com.fasterxml.jackson.databind.JsonNode;
import org.openqa.selenium.By;

public final class Locators {

  private static final String USING = "using";
  private static final String VALUE = "value";
  private static final String OF = "of";
  private static final String MATCHING = "matching";

  private Locators() {
  }

  public static LocatorSpec parse(By by) {
    try {
      if (by instanceof By.Remotable remotable) {
        By.Remotable.Parameters parameters = remotable.getRemoteParameters();
        Strategy strategy = Strategy.fromUsing(parameters.using());
        if (strategy == Strategy.DESCENDANT || strategy == Strategy.ANCESTOR) {
          return parseHierarchy(strategy, String.valueOf(parameters.value()));
        }
        if (strategy != Strategy.OTHER) {
          return new LocatorSpec(strategy, String.valueOf(parameters.value()), null, null);
        }
      }
    } catch (RuntimeException e) {
      return other(by);
    }
    return other(by);
  }

  public static String describe(By by) {
    return describe(parse(by), by.toString());
  }

  private static LocatorSpec parseNode(JsonNode node) {
    Strategy strategy = Strategy.fromUsing(node.get(USING).asText());
    if (strategy == Strategy.DESCENDANT || strategy == Strategy.ANCESTOR) {
      return parseHierarchy(strategy, node.get(VALUE).asText());
    }
    return new LocatorSpec(strategy, node.get(VALUE).asText(), null, null);
  }

  private static LocatorSpec parseHierarchy(Strategy strategy, String json) {
    JsonNode node = JsonConverter.fromString(json, JsonNode.class);
    return new LocatorSpec(strategy, null, parseNode(node.get(OF)), parseNode(node.get(MATCHING)));
  }

  private static LocatorSpec other(By by) {
    return new LocatorSpec(Strategy.OTHER, by.toString(), null, null);
  }

  private static String describe(LocatorSpec spec, String fallback) {
    return switch (spec.strategy()) {
      case OTHER -> fallback;
      case DESCENDANT, ANCESTOR -> label(spec.strategy()) + "(of: " + describe(spec.of(), fallback)
          + ", matching: " + describe(spec.matching(), fallback) + ")";
      default -> label(spec.strategy()) + " \"" + spec.value() + "\"";
    };
  }

  private static String label(Strategy strategy) {
    String using = strategy.using();
    return using.startsWith("-") ? using.substring(1) : using;
  }
}
