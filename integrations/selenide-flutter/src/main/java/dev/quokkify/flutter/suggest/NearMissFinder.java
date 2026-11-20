package dev.quokkify.flutter.suggest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.quokkify.flutter.locator.LocatorSpec;
import dev.quokkify.flutter.locator.Strategy;
import dev.quokkify.flutter.tree.WidgetNode;

import org.apache.commons.text.similarity.LevenshteinDistance;

public final class NearMissFinder {

  public static final int DEFAULT_LIMIT = 5;

  private static final double EXACT_UNHITTABLE_SCORE = 1.0;
  private static final double CASE_SCORE = 0.95;
  private static final double WHITESPACE_SCORE = 0.9;
  private static final double SEMANTICS_LABEL_SCORE = 0.8;
  private static final double PARTIAL_SCORE = 0.7;
  private static final double MIN_SIMILARITY = 0.5;
  private static final String SEMANTICS_HINT = "value found as semantics label; try FlutterBy.semanticsLabel";

  private static final Comparator<NearMiss> BY_SCORE_DESC = Comparator.comparingDouble(NearMiss::score).reversed();

  private NearMissFinder() {
  }

  public static List<NearMiss> find(LocatorSpec spec, WidgetNode root, int limit) {
    if (spec == null || root == null || !spec.strategy().isFlutter() || limit <= 0) {
      return List.of();
    }
    return search(spec, root).stream().sorted(BY_SCORE_DESC).limit(limit).toList();
  }

  private static List<NearMiss> search(LocatorSpec spec, WidgetNode root) {
    return switch (spec.strategy()) {
      case DESCENDANT, ANCESTOR -> searchComposite(spec, root);
      default -> root.flatten().map(node -> score(spec, node)).flatMap(Optional::stream).toList();
    };
  }

  private static List<NearMiss> searchComposite(LocatorSpec spec, WidgetNode root) {
    List<WidgetNode> anchors = resolve(spec.of(), root);
    if (anchors.isEmpty()) {
      return prefixed(search(spec.of(), root), "of: ");
    }
    Set<WidgetNode> candidates = Collections.newSetFromMap(new IdentityHashMap<>());
    for (WidgetNode anchor : anchors) {
      if (spec.strategy() == Strategy.DESCENDANT) {
        anchor.children().forEach(child -> child.flatten().forEach(candidates::add));
      } else {
        candidates.addAll(ancestorsOf(root, anchor));
      }
    }
    return prefixed(candidates.stream().map(node -> score(spec.matching(), node)).flatMap(Optional::stream).toList(),
        "matching: ");
  }

  private static List<WidgetNode> resolve(LocatorSpec spec, WidgetNode root) {
    if (spec.strategy() == Strategy.DESCENDANT || spec.strategy() == Strategy.ANCESTOR) {
      List<WidgetNode> result = new ArrayList<>();
      for (WidgetNode anchor : resolve(spec.of(), root)) {
        if (spec.strategy() == Strategy.DESCENDANT) {
          anchor.children().forEach(child -> child.flatten().filter(n -> matches(spec.matching(), n))
              .forEach(result::add));
        } else {
          ancestorsOf(root, anchor).stream().filter(n -> matches(spec.matching(), n)).forEach(result::add);
        }
      }
      return result;
    }
    return root.flatten().filter(node -> matches(spec, node)).toList();
  }

  private static boolean matches(LocatorSpec spec, WidgetNode node) {
    String actual = field(spec.strategy(), node);
    if (actual == null || !hittable(node)) {
      return false;
    }
    return spec.strategy() == Strategy.TEXT_CONTAINING ? actual.contains(spec.value()) : actual.equals(spec.value());
  }

  private static List<WidgetNode> ancestorsOf(WidgetNode root, WidgetNode target) {
    List<WidgetNode> path = new ArrayList<>();
    findPath(root, target, path);
    return path;
  }

  private static boolean findPath(WidgetNode current, WidgetNode target, List<WidgetNode> path) {
    if (current == target) {
      return true;
    }
    path.add(current);
    for (WidgetNode child : current.children()) {
      if (findPath(child, target, path)) {
        return true;
      }
    }
    path.remove(path.size() - 1);
    return false;
  }

  private static List<NearMiss> prefixed(List<NearMiss> misses, String prefix) {
    return misses.stream()
        .map(m -> new NearMiss(m.node(), m.score(), m.reasons().stream().map(r -> prefix + r).toList()))
        .toList();
  }

  private static Optional<NearMiss> score(LocatorSpec spec, WidgetNode node) {
    Strategy strategy = spec.strategy();
    String wanted = spec.value();
    String actual = field(strategy, node);
    if (actual != null) {
      Optional<NearMiss> byField = scoreField(node, fieldName(strategy), wanted, actual);
      if (byField.isPresent()) {
        return byField;
      }
    }
    if ((strategy == Strategy.TEXT || strategy == Strategy.KEY) && wanted.equals(node.semanticsLabel())) {
      return Optional.of(new NearMiss(node, SEMANTICS_LABEL_SCORE, List.of(SEMANTICS_HINT)));
    }
    return Optional.empty();
  }

  private static Optional<NearMiss> scoreField(WidgetNode node, String name, String wanted, String actual) {
    if (actual.equals(wanted)) {
      return hittable(node) ? Optional.empty()
          : Optional.of(new NearMiss(node, EXACT_UNHITTABLE_SCORE, List.of(unhittableReason(node, name))));
    }
    if (actual.equalsIgnoreCase(wanted)) {
      return Optional.of(new NearMiss(node, CASE_SCORE, List.of("differs only in case")));
    }
    if (actual.strip().equals(wanted.strip())) {
      return Optional.of(new NearMiss(node, WHITESPACE_SCORE, List.of("differs only in whitespace")));
    }
    if (actual.contains(wanted) || wanted.contains(actual)) {
      return Optional.of(new NearMiss(node, PARTIAL_SCORE, List.of("partial match")));
    }
    int distance = LevenshteinDistance.getDefaultInstance().apply(actual, wanted);
    double similarity = 1.0 - (double) distance / Math.max(actual.length(), wanted.length());
    if (similarity >= MIN_SIMILARITY) {
      return Optional.of(new NearMiss(node, similarity, List.of("similar " + name + " (distance " + distance + ")")));
    }
    return Optional.empty();
  }

  private static String unhittableReason(WidgetNode node, String name) {
    List<String> problems = new ArrayList<>();
    if (!node.visible()) {
      problems.add("hidden");
    }
    if (!node.enabled()) {
      problems.add("disabled");
    }
    if (node.rect().isEmpty()) {
      problems.add("zero-size");
    }
    return "exact " + name + " match but " + String.join(" and ", problems);
  }

  private static boolean hittable(WidgetNode node) {
    return node.visible() && node.enabled() && !node.rect().isEmpty();
  }

  private static String field(Strategy strategy, WidgetNode node) {
    return switch (strategy) {
      case KEY -> node.key();
      case TEXT, TEXT_CONTAINING -> node.text();
      case SEMANTICS_LABEL -> node.semanticsLabel();
      case TYPE -> node.type();
      default -> null;
    };
  }

  private static String fieldName(Strategy strategy) {
    return switch (strategy) {
      case KEY -> "key";
      case TEXT, TEXT_CONTAINING -> "text";
      case SEMANTICS_LABEL -> "semantics label";
      case TYPE -> "type";
      default -> throw new IllegalArgumentException("No comparable field for " + strategy);
    };
  }
}
