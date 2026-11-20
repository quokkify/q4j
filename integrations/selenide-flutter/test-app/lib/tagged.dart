import 'package:flutter/widgets.dart';

/// Gives [child] a stable id: a `ValueKey(id)` for the Flutter finder and a
/// Semantics identifier (Android resource-id) for native UiAutomator2.
///
/// `container: true` keeps the identifier on its own accessibility node, so a
/// tagged text still exposes its text and never merges into its parent.
class Tagged extends StatelessWidget {
  final String id;
  final Widget child;

  const Tagged(this.id, this.child, {super.key});

  @override
  Widget build(BuildContext context) {
    return KeyedSubtree(
      key: ValueKey(id),
      child: Semantics(container: true, identifier: id, child: child),
    );
  }
}
