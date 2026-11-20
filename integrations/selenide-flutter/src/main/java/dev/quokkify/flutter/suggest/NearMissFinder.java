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
  private static final String NOT_HIT_TESTABLE = " match exists in the widget tree but is not hit-testable";
  // Real Flutter server trees expose no offstage/visible/opacity/ignoring flags, so these ancestors are detected by
  // widget type alone; whether they actually hide the widget is not known.
  private static final Set<String> HIDING_ANCESTORS =
      Set.of("Offstage", "Visibility", "Opacity", "IgnorePointer", "AbsorbPointer");

  private static final Comparator<NearMiss> BY_SCORE_DESC = Comparator.comparingDouble(NearMiss::score).reversed();

  private NearMissFinder() {
  }

  public static List<NearMiss> find(LocatorSpec spec, WidgetNode root, int limit) {
    if (spec == null || root == null || !spec.strategy().isFlutter() || limit <= 0) {
      return List.of();
    }
    return deepestExactMatches(search(spec, root)).stream().sorted(BY_SCORE_DESC).limit(limit).toList();
  }

  private static List<NearMiss> search(LocatorSpec spec, WidgetNode root) {
    return switch (spec.strategy()) {
      case DESCENDANT, ANCESTOR -> searchComposite(spec, root);
      default -> root.flatten().map(node -> score(spec, node, root)).flatMap(Optional::stream).toList();
    };
  }

  /**
   * Real trees copy a widget's text onto every wrapper above it, so one widget yields a chain of exact matches; only
   * the deepest one of each chain is kept.
   */
  private static List<NearMiss> deepestExactMatches(List<NearMiss> misses) {
    List<WidgetNode> exact = misses.stream().filter(m -> m.score() == EXACT_UNHITTABLE_SCORE).map(NearMiss::node)
        .toList();
    return misses.stream()
        .filter(m -> m.score() != EXACT_UNHITTABLE_SCORE || exact.stream().noneMatch(other -> other != m.node()
            && m.node().flatten().anyMatch(descendant -> descendant == other)))
        .toList();
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
    return prefixed(candidates.stream().map(node -> score(spec.matching(), node, root)).flatMap(Optional::stream)
        .toList(), "matching: ");
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

  private static Optional<NearMiss> score(LocatorSpec spec, WidgetNode node, WidgetNode root) {
    Strategy strategy = spec.strategy();
    String wanted = spec.value();
    String actual = field(strategy, node);
    if (actual != null) {
      Optional<NearMiss> byField = scoreField(strategy, node, root, fieldName(strategy), wanted, actual);
      if (byField.isPresent()) {
        return byField;
      }
    }
    if ((strategy == Strategy.TEXT || strategy == Strategy.KEY) && wanted.equals(node.semanticsLabel())) {
      return Optional.of(new NearMiss(node, SEMANTICS_LABEL_SCORE, List.of(SEMANTICS_HINT)));
    }
    return Optional.empty();
  }

  private static Optional<NearMiss> scoreField(Strategy strategy, WidgetNode node, WidgetNode root, String name,
                                             String wanted, String actual) {
    boolean exact = actual.equals(wanted) || strategy == Strategy.TEXT_CONTAINING && actual.contains(wanted);
    if (exact) {
      // The find failed, so an exact match in the tree is a widget the driver's hit test did not accept.
      return Optional.of(new NearMiss(node, EXACT_UNHITTABLE_SCORE, unhittableReasons(node, root, name)));
    }
    if (actual.equalsIgnoreCase(wanted)) {
      return Optional.of(new NearMiss(node, CASE_SCORE, List.of("differs only in case")));
    }
    if (actual.strip().equals(wanted.strip())) {
      return Optional.of(new NearMiss(node, WHITESPACE_SCORE, List.of("differs only in whitespace")));
    }
    if (!actual.isBlank() && !wanted.isBlank() && (actual.contains(wanted) || wanted.contains(actual))) {
      return Optional.of(new NearMiss(node, PARTIAL_SCORE, List.of("partial match")));
    }
    int distance = LevenshteinDistance.getDefaultInstance().apply(actual, wanted);
    double similarity = 1.0 - (double) distance / Math.max(actual.length(), wanted.length());
    if (similarity >= MIN_SIMILARITY) {
      return Optional.of(new NearMiss(node, similarity, List.of("similar " + name + " (distance " + distance + ")")));
    }
    return Optional.empty();
  }

  private static List<String> unhittableReasons(WidgetNode node, WidgetNode root, String name) {
    List<String> problems = new ArrayList<>();
    problems.add("exact " + name + NOT_HIT_TESTABLE);
    if (!node.visible()) {
      problems.add("hidden");
    }
    if (!node.enabled()) {
      problems.add("disabled");
    }
    if (node.rect().isEmpty()) {
      problems.add("zero-size");
    }
    ancestorsOf(root, node).stream()
        .map(WidgetNode::type)
        .filter(HIDING_ANCESTORS::contains)
        .distinct()
        .forEach(type -> problems.add("hidden by " + type + " ancestor (detected by widget type)"));
    return problems;
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
