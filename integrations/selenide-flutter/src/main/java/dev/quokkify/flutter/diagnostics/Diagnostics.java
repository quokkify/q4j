package dev.quokkify.flutter.diagnostics;

import java.util.List;

import dev.quokkify.flutter.suggest.NearMiss;

public record Diagnostics(String treeJson, String treeText, List<NearMiss> suggestions, String error,
                          boolean truncated) {

  public Diagnostics {
    suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
  }
}
