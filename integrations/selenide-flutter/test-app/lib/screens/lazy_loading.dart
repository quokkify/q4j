import 'package:appium_testing_app/components/custom_app_bar.dart';
import 'package:appium_testing_app/tagged.dart';
import 'package:material_ui/material_ui.dart';

class LazyLoadingScreen extends StatefulWidget {
  final String title;

  const LazyLoadingScreen({required this.title, super.key});

  @override
  State<LazyLoadingScreen> createState() => _LazyLoadingScreenState();
}

class _LazyLoadingScreenState extends State<LazyLoadingScreen> {
  static const int _itemCount = 100;

  int? _selected;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomAppBarWidget(title: widget.title),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              height: 30,
              child: _selected == null
                  ? null
                  : Tagged("list_selected", Text("Selected $_selected")),
            ),
            Expanded(
              child: Tagged(
                "list_view",
                ListView.builder(
                  itemCount: _itemCount,
                  itemBuilder: (context, index) {
                    return Tagged(
                      "list_item_$index",
                      ListTile(
                        title: Text("Item $index"),
                        onTap: () {
                          setState(() {
                            _selected = index;
                          });
                        },
                      ),
                    );
                  },
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
