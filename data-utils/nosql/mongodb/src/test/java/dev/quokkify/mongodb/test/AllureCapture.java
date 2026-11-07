package dev.quokkify.mongodb.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;

/**
 * Runs an action inside a temporary in-memory Allure lifecycle and returns the recorded attachments.
 */
final class AllureCapture {

  private AllureCapture() {
  }

  /**
   * Run the action and collect attachments.
   *
   * @param action action to run
   * @return attachment contents by attachment name, in recording order
   */
  static List<Map.Entry<String, String>> run(Runnable action) {
    InMemoryResultsWriter writer = new InMemoryResultsWriter();
    AllureLifecycle previous = Allure.getLifecycle();
    AllureLifecycle lifecycle = new AllureLifecycle(writer);
    Allure.setLifecycle(lifecycle);
    try {
      AllureExternalKey key = AllureExternalKey.random(AllureCapture.class);
      lifecycle.scheduleTest(key, new TestResult().setUuid(UUID.randomUUID().toString()).setName("attachments capture"));
      lifecycle.startTest(key);
      try {
        action.run();
      } finally {
        lifecycle.stopTest(key);
        lifecycle.writeTest(key);
      }
    } finally {
      Allure.setLifecycle(previous);
    }
    List<Map.Entry<String, String>> attachments = new ArrayList<>();
    writer.results.forEach(result -> collect(result.getAttachments(), result.getSteps(), writer, attachments));
    return attachments;
  }

  private static void collect(List<Attachment> attachments, List<StepResult> steps, InMemoryResultsWriter writer,
      List<Map.Entry<String, String>> collected) {
    attachments.forEach(attachment ->
        collected.add(Map.entry(attachment.getName(), writer.contents.get(attachment.getSource()))));
    steps.forEach(step -> collect(step.getAttachments(), step.getSteps(), writer, collected));
  }

  private static final class InMemoryResultsWriter implements AllureResultsWriter {
    private final List<TestResult> results = new ArrayList<>();
    private final Map<String, String> contents = new HashMap<>();

    @Override
    public void write(TestResult testResult) {
      results.add(testResult);
    }

    @Override
    public void write(io.qameta.allure.model.TestResultContainer testResultContainer) {
      // containers are not needed
    }

    @Override
    public void write(String source, InputStream attachment) {
      try {
        contents.put(source, new String(attachment.readAllBytes(), StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read attachment " + source, e);
      }
    }
  }
}
