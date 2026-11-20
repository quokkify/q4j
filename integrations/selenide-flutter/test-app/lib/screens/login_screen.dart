import 'package:appium_testing_app/components/custom_app_bar.dart';
import 'package:appium_testing_app/tagged.dart';
import 'package:material_ui/material_ui.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  TextEditingController emailController = TextEditingController();
  TextEditingController passwordController = TextEditingController();
  String? _welcomeEmail;

  @override
  void dispose() {
    emailController.dispose();
    passwordController.dispose();
    super.dispose();
  }

  bool get _canSubmit =>
      emailController.text.contains("@") &&
      passwordController.text.length >= 6;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const CustomAppBarWidget(
        title: "Login",
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              "Please Login",
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w500),
            ),
            const SizedBox(
              height: 20,
            ),
            Tagged(
              "login_email",
              TextField(
                decoration: const InputDecoration.collapsed(hintText: "Email"),
                controller: emailController,
                onChanged: (_) => setState(() {}),
              ),
            ),
            const SizedBox(
              height: 30,
            ),
            Tagged(
              "login_password",
              TextField(
                decoration:
                    const InputDecoration.collapsed(hintText: "Password"),
                obscureText: true,
                controller: passwordController,
                onChanged: (_) => setState(() {}),
              ),
            ),
            const SizedBox(
              height: 20,
            ),
            SizedBox(
              width: double.infinity,
              child: Tagged(
                "login_button",
                ElevatedButton(
                  onPressed: _canSubmit
                      ? () {
                          setState(() {
                            _welcomeEmail = emailController.text;
                          });
                        }
                      : null,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.primary,
                  ),
                  child: const Text(
                    "Login",
                    style: TextStyle(color: Colors.white),
                  ),
                ),
              ),
            ),
            const SizedBox(
              height: 30,
            ),
            if (_welcomeEmail != null)
              Tagged("login_welcome", Text("Welcome, $_welcomeEmail")),
          ],
        ),
      ),
    );
  }
}
