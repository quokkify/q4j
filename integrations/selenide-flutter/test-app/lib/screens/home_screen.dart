import 'package:appium_testing_app/components/custom_app_bar.dart';
import 'package:appium_testing_app/models/feature_model.dart';
import 'package:appium_testing_app/screens/duplicates_screen.dart';
import 'package:appium_testing_app/screens/label_only_screen.dart';
import 'package:appium_testing_app/screens/lazy_loading.dart';
import 'package:appium_testing_app/screens/loader_screen.dart';
import 'package:appium_testing_app/screens/login_screen.dart';
import 'package:appium_testing_app/screens/offstage_screen.dart';
import 'package:appium_testing_app/screens/ui_elements_screen.dart';
import 'package:appium_testing_app/tagged.dart';
import 'package:material_ui/material_ui.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<FeatureModel> featureModels = [];

  @override
  void initState() {
    super.initState();
    featureModels.add(FeatureModel(
        id: "home_login",
        title: "Login",
        subtitle: "Form with a validated submit button",
        page: (context) => const LoginScreen()));
    featureModels.add(FeatureModel(
        id: "home_delayed",
        title: "Delayed",
        subtitle: "Result appears 2 seconds after a tap",
        page: (context) => const LoaderScreen(title: "Delayed")));
    featureModels.add(FeatureModel(
        id: "home_list",
        title: "Long List",
        subtitle: "Lazily built list of 100 items",
        page: (context) => const LazyLoadingScreen(title: "Long List")));
    featureModels.add(FeatureModel(
        id: "home_duplicates",
        title: "Duplicates",
        subtitle: "Two cards with the same button",
        page: (context) => const DuplicatesScreen(title: "Duplicates")));
    featureModels.add(FeatureModel(
        id: "home_label",
        title: "Label Only",
        subtitle: "Icon button found by its semantics label",
        page: (context) => const LabelOnlyScreen(title: "Label Only")));
    featureModels.add(FeatureModel(
        id: "home_offstage",
        title: "Offstage",
        subtitle: "Hidden and covered widgets",
        page: (context) => const OffstageScreen(title: "Offstage")));
    featureModels.add(FeatureModel(
        title: "UI Elements",
        subtitle: "Demos different ui elements with different states",
        page: (context) => const UiElementsScreen(title: "UI Elements")));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: const CustomAppBarWidget(
          title: "Samples List",
        ),
        body: ListView.separated(
          itemCount: featureModels.length,
          itemBuilder: (context, index) {
            final feature = featureModels[index];
            final tile = ListTile(
              title: Text(feature.title),
              subtitle: Text(feature.subtitle),
              onTap: () {
                Navigator.of(context)
                    .push(MaterialPageRoute(builder: feature.page));
              },
            );
            final id = feature.id;
            return id == null ? tile : Tagged(id, tile);
          },
          separatorBuilder: (BuildContext context, int index) =>
              const Divider(),
        ));
  }
}
