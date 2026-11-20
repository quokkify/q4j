package dev.quokkify.flutter.locator;

public record LocatorSpec(Strategy strategy, String value, LocatorSpec of, LocatorSpec matching) {
}
