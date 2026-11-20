package dev.quokkify.flutter.locator;

public enum Strategy {
  KEY("-flutter key"),
  TEXT("-flutter text"),
  TEXT_CONTAINING("-flutter text containing"),
  TYPE("-flutter type"),
  SEMANTICS_LABEL("-flutter semantics label"),
  DESCENDANT("-flutter descendant"),
  ANCESTOR("-flutter ancestor"),
  ID("id"),
  ACCESSIBILITY_ID("accessibility id"),
  OTHER(null);

  private static final String FLUTTER_PREFIX = "-flutter ";

  private final String using;

  Strategy(String using) {
    this.using = using;
  }

  public boolean isFlutter() {
    return using != null && using.startsWith(FLUTTER_PREFIX);
  }

  static Strategy fromUsing(String using) {
    for (Strategy strategy : values()) {
      if (strategy.using != null && strategy.using.equals(using)) {
        return strategy;
      }
    }
    return OTHER;
  }

  String using() {
    return using;
  }
}
