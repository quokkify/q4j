package dev.quokkify.flutter.tree;

import java.util.List;
import java.util.stream.Stream;

public record WidgetNode(String type, String elementType, String description, int depth, String key, String text,
                         String semanticsLabel, String tooltip, String hintText, boolean visible, boolean enabled,
                         boolean focused, Rect rect, List<WidgetNode> children) {

  public Stream<WidgetNode> flatten() {
    return Stream.concat(Stream.of(this), children.stream().flatMap(WidgetNode::flatten));
  }
}
