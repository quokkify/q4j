package dev.quokkify.reportportal.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import dev.quokkify.model.JsonPojo;
import dev.quokkify.reportportal.config.ReportPortalConnectionConfig;
import dev.quokkify.reportportal.e2e.sample.ReportedSampleTest;
import dev.quokkify.reportportal.listeners.ReportPortalListener;

import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.Response;
import io.qameta.allure.TmsLink;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.TestNG;
import org.testng.annotations.Test;

import static dev.quokkify.reportportal.test.ReportPortalTestSupport.API;
import static dev.quokkify.reportportal.test.ReportPortalTestSupport.PROJECT;
import static dev.quokkify.reportportal.test.ReportPortalTestSupport.json;
import static org.assertj.core.api.Assertions.assertThat;

public class ReportPortalAgentEndToEndTest {

  private static final Logger LOG = LoggerFactory.getLogger(ReportPortalAgentEndToEndTest.class);
  private static final Duration CHILD_RUN_TIMEOUT = Duration.ofMinutes(3);

  @TmsLink("RP_E2E_1")
  @Test(description = "ReportPortalListener reports a TestNG run as a launch with step statuses and TMS description")
  public void shouldReportTestNgRunThroughAgent() throws IOException, InterruptedException {
    String launchName = "q4j-agent-e2e-" + UUID.randomUUID();

    try {
      runSampleSuiteInChildJvm(launchName);

      Awaitility.await()
          .atMost(Duration.ofSeconds(60))
          .pollInterval(Duration.ofSeconds(1))
          .untilAsserted(() -> assertReportedLaunch(launchName));
    } finally {
      deleteLaunchByName(launchName);
    }
  }

  private static void deleteLaunchByName(String launchName) {
    try {
      JsonPojo launches = json(API.findLaunches(PROJECT, launchName));
      if (launches.at("/content/0/id").isMissingNode()) {
        return;
      }
      long launchId = launches.requiredAt("/content/0/id").asLong();
      if ("IN_PROGRESS".equals(launches.at("/content/0/status").asText())) {
        String stopBody = new JsonPojo()
            .setField("endTime", Instant.now().toString())
            .setField("status", "STOPPED")
            .asJson();
        try (Response ignored = API.stopLaunch(PROJECT, launchId, stopBody)) {
          LOG.debug("Stopped launch {}", launchId);
        }
      }
      ReportPortalTestSupport.deleteLaunch(launchId);
    } catch (Exception e) {
      LOG.warn("Failed to delete launch '{}': {}", launchName, e.getMessage());
    }
  }

  private static void assertReportedLaunch(String launchName) {
    JsonPojo launches = json(API.findLaunches(PROJECT, launchName));
    assertThat(launches.at("/content/0/status").asText()).as("Launch status").isEqualTo("FAILED");
    assertThat(launches.at("/content/0/statistics/executions/total").asInt()).as("Total executions").isEqualTo(3);
    assertThat(launches.at("/content/0/statistics/executions/passed").asInt()).as("Passed executions").isEqualTo(1);
    assertThat(launches.at("/content/0/statistics/executions/failed").asInt()).as("Failed executions").isEqualTo(1);
    assertThat(launches.at("/content/0/statistics/executions/skipped").asInt()).as("Skipped executions").isEqualTo(1);

    JsonPojo steps = json(API.getSteps(PROJECT, launches.requiredAt("/content/0/id").asLong()));
    assertThat(steps.requiredAt("/content").size()).as("Reported steps").isEqualTo(3);
    assertThat(stepStatus(steps, "passingTest")).isEqualTo("PASSED");
    assertThat(stepStatus(steps, "failingTest")).isEqualTo("FAILED");
    assertThat(stepStatus(steps, "skippedTest")).isEqualTo("SKIPPED");
    assertThat(step(steps, "passingTest").path("description").asText())
        .as("ParamOverrideTestNgService should add the TMS link and description")
        .contains("**Test Case ID:** [RP_E2E_SAMPLE]")
        .contains("**Description:** Sample passing test");
  }

  private static ObjectNode step(JsonPojo steps, String name) {
    return steps.findFirstObjectByFieldValue("name", name)
        .orElseThrow(() -> new AssertionError("Step '%s' is not reported, got: %s".formatted(name, steps.asJson())));
  }

  private static String stepStatus(JsonPojo steps, String name) {
    return step(steps, name).path("status").asText();
  }

  private static void runSampleSuiteInChildJvm(String launchName) throws IOException, InterruptedException {
    Path workDir = Files.createTempDirectory("rp-agent-e2e");
    File output = workDir.resolve("child.log").toFile();
    List<String> command = List.of(
        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
        "-cp", System.getProperty("java.class.path"),
        "-DRUN_REPORT_PORTAL=true",
        "-DRP_PROJECT_NAME=" + PROJECT,
        "-DRP_LAUNCH_NAME=" + launchName,
        "-DRP_LAUNCH_MODE=DEFAULT",
        "-Drp.endpoint=" + ReportPortalConnectionConfig.ENDPOINT,
        TestNG.class.getName(),
        "-usedefaultlisteners", "false",
        "-d", workDir.resolve("testng-output").toString(),
        "-listener", ReportPortalListener.class.getName(),
        "-testclass", ReportedSampleTest.class.getName());

    ProcessBuilder builder = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .redirectOutput(output);
    builder.environment().put("RP_API_KEY", ReportPortalConnectionConfig.API_KEY);
    try {
      Process process = builder.start();
      boolean finished = process.waitFor(CHILD_RUN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      if (!finished) {
        process.destroyForcibly().waitFor();
      }
      assertThat(finished)
          .as("Child TestNG run should finish within %s, output:%n%s", CHILD_RUN_TIMEOUT,
              Files.readString(output.toPath(), StandardCharsets.UTF_8))
          .isTrue();
    } finally {
      deleteRecursively(workDir);
    }
  }

  private static void deleteRecursively(Path dir) throws IOException {
    try (Stream<Path> paths = Files.walk(dir)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    }
  }
}
