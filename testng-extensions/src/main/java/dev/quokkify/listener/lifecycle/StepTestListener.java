package dev.quokkify.listener.lifecycle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import io.qameta.allure.AllureResultsWriteException;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.PropertiesUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This listener logs test steps and cleans attachments for successful tests.
 */
public class StepTestListener implements StepLifecycleListener, TestLifecycleListener {

  private static final ThreadLocal<TestResult> TEST_RESULT_THREAD_LOCAL = new ThreadLocal<>();
  private static final Logger LOG = LogManager.getLogger();

  private static final Properties properties = PropertiesUtils.loadAllureProperties();
  private static final Path outputDirectory = Paths.get(
      properties.getProperty("allure.results.directory", "allure-results"));

  @Override
  public void beforeStepStop(StepResult result) {
    if (Objects.nonNull(TEST_RESULT_THREAD_LOCAL.get())) {
      String testMethod = TEST_RESULT_THREAD_LOCAL.get().getLabels().stream()
          .filter(label -> label.getName().equals("testMethod"))
          .findFirst()
          .orElseThrow(() -> new RuntimeException("There is no testMethod label in StepResult"))
          .getValue();
      LOG.debug("[{}] [{}] {}", result.getStatus(), testMethod, result.getName());
    } else {
      LOG.debug("[{}] {}", result.getStatus(), result.getName());
    }
  }

  @Override
  public void beforeTestStart(TestResult result) {
    TEST_RESULT_THREAD_LOCAL.set(result);
  }

  @Override
  public void beforeTestWrite(TestResult testResult) {
    if (testResult.getStatus().equals(Status.PASSED)) {
      removeMatchingAttachments(testResult.getAttachments());
      testResult.getSteps().stream()
          .flatMap(this::getSubSteps)
          .map(StepResult::getAttachments)
          .forEach(this::removeMatchingAttachments);
    }
  }

  private void removeMatchingAttachments(Collection<Attachment> attachments) {
    List<Attachment> attachmentsToRemove = attachments.stream().toList();
    attachmentsToRemove.forEach(this::deleteAttachmentFile);
    attachments.removeAll(attachmentsToRemove);
  }

  private Stream<StepResult> getSubSteps(StepResult step) {
    if (step.getSteps().isEmpty()) {
      return Stream.of(step);
    }
    return step.getSteps().stream().flatMap(this::getSubSteps);
  }

  private void deleteAttachmentFile(Attachment attachment) {
    Path filePath = outputDirectory.resolve(attachment.getSource());
    try {
      Files.delete(filePath);
    } catch (IOException exception) {
      throw new AllureResultsWriteException("Couldn't remove Allure attachment", exception);
    }
  }
}
