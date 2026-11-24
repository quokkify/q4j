package dev.quokkify.flutter.tree;

public record Rect(double x, double y, double width, double height) {

  public boolean isEmpty() {
    return width <= 0 || height <= 0;
  }
}
