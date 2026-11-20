import 'package:appium_testing_app/components/custom_app_bar.dart';
import 'package:appium_testing_app/tagged.dart';
import 'package:material_ui/material_ui.dart';

class LoaderScreen extends StatefulWidget {
  final String title;
  const LoaderScreen({required this.title, super.key});

  @override
  State<LoaderScreen> createState() => _LoaderScreenState();
}

class _LoaderScreenState extends State<LoaderScreen> {
  static const Duration _delay = Duration(seconds: 2);

  bool _showText = false;
  bool _showLoader = false;

  void _load() {
    setState(() {
      _showLoader = true;
      _showText = false;
    });
    Future.delayed(_delay, () {
      if (!mounted) return;
      setState(() {
        _showLoader = false;
        _showText = true;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomAppBarWidget(title: widget.title),
      body: Center(
        child: Column(
          children: [
            const SizedBox(
              height: 30,
            ),
            _showLoader ? const CircularProgressIndicator() : const SizedBox(),
            Tagged(
              "delayed_load",
              ElevatedButton(
                onPressed: _showLoader ? null : _load,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Theme.of(context).colorScheme.primary,
                ),
                child: const Text(
                  "Load",
                  style: TextStyle(color: Colors.white),
                ),
              ),
            ),
            const SizedBox(
              height: 30,
            ),
            _showText
                ? const Tagged("delayed_result", Text("Loaded"))
                : const SizedBox(),
          ],
        ),
      ),
    );
  }
}
