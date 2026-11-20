package dev.quokkify.flutter.tree;

public final class WidgetTreeFormatter {

  private static final String INDENT = "  ";

  private WidgetTreeFormatter() {
  }

  public static String format(WidgetNode root) {
    StringBuilder out = new StringBuilder();
    append(out, root);
    return out.toString();
  }

  private static void append(StringBuilder out, WidgetNode node) {
    out.append(INDENT.repeat(node.depth())).append(node.type());
    appendAttribute(out, "key", node.key());
    appendAttribute(out, "text", node.text());
    appendAttribute(out, "label", node.semanticsLabel());
    if (!node.visible()) {
      out.append(" [hidden]");
    }
    if (!node.enabled()) {
      out.append(" [disabled]");
    }
    if (node.rect().isEmpty()) {
      out.append(" [zero-size]");
    }
    out.append(String.format(" @%d,%d %dx%d%n", Math.round(node.rect().x()), Math.round(node.rect().y()),
        Math.round(node.rect().width()), Math.round(node.rect().height())));
    node.children().forEach(child -> append(out, child));
  }

  private static void appendAttribute(StringBuilder out, String name, String value) {
    if (value != null && !value.isEmpty()) {
      out.append(' ').append(name).append("=\"").append(value).append('"');
    }
  }
}
