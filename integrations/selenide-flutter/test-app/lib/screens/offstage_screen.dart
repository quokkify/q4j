import 'package:appium_testing_app/components/custom_app_bar.dart';
import 'package:appium_testing_app/tagged.dart';
import 'package:material_ui/material_ui.dart';

class OffstageScreen extends StatelessWidget {
  final String title;

  const OffstageScreen({required this.title, super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomAppBarWidget(title: title),
      body: Stack(
        children: [
          Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Offstage(
                  offstage: true,
                  child: Tagged("hidden_banner", Text("Hidden banner")),
                ),
                Tagged(
                  "covered_button",
                  ElevatedButton(
                    onPressed: () {},
                    child: const Text("Covered"),
                  ),
                ),
              ],
            ),
          ),
          Positioned.fill(
            child: Container(
              color: Colors.white,
            ),
          ),
        ],
      ),
    );
  }
}
