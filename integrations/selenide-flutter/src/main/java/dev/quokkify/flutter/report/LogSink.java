package dev.quokkify.flutter.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogSink implements DiagnosticsSink {

  public static final String SUMMARY_NAME = "Flutter: element search";

  private static final Logger LOG = LoggerFactory.getLogger(LogSink.class);

  @Override
  public void attach(String name, String mimeType, String content) {
    if (SUMMARY_NAME.equals(name)) {
      LOG.info("{}:{}{}", name, System.lineSeparator(), content);
    } else {
      LOG.warn("{}:{}{}", name, System.lineSeparator(), content);
    }
  }
}
