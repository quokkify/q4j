package dev.quokkify.flutter.report;

import dev.quokkify.flutter.FlutterVisibility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogSink implements DiagnosticsSink {

  public static final String SUMMARY_NAME = "Flutter: element search";

  private static final String JSON = "application/json";
  private static final Logger LOG = LoggerFactory.getLogger(LogSink.class);

  @Override
  public void attach(String name, String mimeType, String content) {
    if (isOmitted(name, mimeType)) {
      LOG.info("{}: JSON omitted from the log; see the test report attachment", name);
    } else if (SUMMARY_NAME.equals(name)) {
      LOG.info("{}:{}{}", name, System.lineSeparator(), content);
    } else {
      LOG.warn("{}:{}{}", name, System.lineSeparator(), content);
    }
  }

  static boolean isOmitted(String name, String mimeType) {
    return JSON.equals(mimeType) || FlutterVisibility.TREE_JSON.equals(name);
  }
}
