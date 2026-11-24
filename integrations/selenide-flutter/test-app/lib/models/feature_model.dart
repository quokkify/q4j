import 'package:flutter/widgets.dart';

class FeatureModel {
  String? id;
  String title;
  String subtitle;
  WidgetBuilder page;

  FeatureModel({
    this.id,
    required this.title,
    required this.subtitle,
    required this.page,
  });
}
