import 'package:appium_testing_app/components/custom_app_bar.dart';
import 'package:appium_testing_app/tagged.dart';
import 'package:material_ui/material_ui.dart';

class LabelOnlyScreen extends StatefulWidget {
  final String title;

  const LabelOnlyScreen({required this.title, super.key});

  @override
  State<LabelOnlyScreen> createState() => _LabelOnlyScreenState();
}

class _LabelOnlyScreenState extends State<LabelOnlyScreen> {
  bool _opened = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomAppBarWidget(title: widget.title),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Semantics(
              label: "Settings icon",
              child: IconButton(
                icon: const Icon(Icons.settings),
                onPressed: () {
                  setState(() {
                    _opened = true;
                  });
                },
              ),
            ),
            const SizedBox(
              height: 30,
            ),
            if (_opened) const Tagged("label_result", Text("Settings opened")),
          ],
        ),
      ),
    );
  }
}
