package dev.quokkify.flutter.suggest;

import java.util.List;

import dev.quokkify.flutter.tree.WidgetNode;

public record NearMiss(WidgetNode node, double score, List<String> reasons) {
}
