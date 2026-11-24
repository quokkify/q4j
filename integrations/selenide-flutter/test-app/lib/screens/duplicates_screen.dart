import 'package:appium_testing_app/components/custom_app_bar.dart';
import 'package:appium_testing_app/tagged.dart';
import 'package:material_ui/material_ui.dart';

class DuplicatesScreen extends StatefulWidget {
  final String title;

  const DuplicatesScreen({required this.title, super.key});

  @override
  State<DuplicatesScreen> createState() => _DuplicatesScreenState();
}

class _DuplicatesScreenState extends State<DuplicatesScreen> {
  String? _opened;

  Widget _card(String id, String name) {
    return Tagged(
      id,
      Card(
        child: ListTile(
          title: Text("Card $name"),
          trailing: TextButton(
            onPressed: () {
              setState(() {
                _opened = name;
              });
            },
            child: const Text("Details"),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomAppBarWidget(title: widget.title),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _card("card_a", "A"),
            const SizedBox(
              height: 20,
            ),
            _card("card_b", "B"),
            const SizedBox(
              height: 30,
            ),
            if (_opened != null)
              Tagged("details_result", Text("Opened $_opened")),
          ],
        ),
      ),
    );
  }
}
