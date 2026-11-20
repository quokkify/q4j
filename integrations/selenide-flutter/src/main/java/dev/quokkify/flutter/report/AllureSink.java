package dev.quokkify.flutter.report;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AttachmentOptions;

public final class AllureSink implements DiagnosticsSink {

  private static final String JSON = "application/json";

  @Override
  public void attach(String name, String mimeType, String content) {
    AllureLifecycle lifecycle = Allure.getLifecycle();
    if (lifecycle.getCurrentExecutableKey().isEmpty()) {
      return;
    }
    lifecycle.addAttachment(name, mimeType, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
        AttachmentOptions.withFileExtension(JSON.equals(mimeType) ? ".json" : ".txt"));
  }
}
