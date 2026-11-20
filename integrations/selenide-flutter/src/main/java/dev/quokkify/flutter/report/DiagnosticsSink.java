package dev.quokkify.flutter.report;

public interface DiagnosticsSink {

  void attach(String name, String mimeType, String content);
}
