package dev.quokkify.reportportal.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import dev.quokkify.model.JsonPojo;
import dev.quokkify.reportportal.config.ReportPortalConnectionConfig;
import dev.quokkify.reportportal.e2e.sample.ReportedSampleTest;
import dev.quokkify.reportportal.listeners.ReportPortalListener;

import com.fasterxml.jackson.databind.JsonNode;
import io.qameta.allure.TmsLink;
import org.awaitility.Awaitility;
import org.testng.TestNG;
import org.testng.annotations.Test;

import static dev.quokkify.reportportal.test.ReportPortalTestSupport.API;
import static dev.quokkify.reportportal.test.ReportPortalTestSupport.PROJECT;
import static dev.quokkify.reportportal.test.ReportPortalTestSupport.json;
import static org.assertj.core.api.Assertions.assertThat;

public class ReportPortalAgentEndToEndTest {

  private static final Duration CHILD_RUN_TIMEOUT = Duration.ofMinutes(3);

  @TmsLink("RP_E2E_1")
  @Test(description = "ReportPortalListener reports a TestNG run as a launch with step statuses and TMS description")
  public void shouldReportTestNgRunThroughAgent() throws IOException, InterruptedException {
    String launchName = "q4j-agent-e2e-" + UUID.randomUUID();

    runSampleSuiteInChildJvm(launchName);

    Awaitility.await()
        .atMost(Duration.ofSeconds(60))
        .pollInterval(Duration.ofSeconds(1))
        .untilAsserted(() -> assertReportedLaunch(launchName));
  }

  private static void assertReportedLaunch(String launchName) {
    JsonNode launch = json(API.findLaunches(PROJECT, launchName)).json().path("content").path(0);
    assertThat(launch.path("status").asText()).as("Launch status").isEqualTo("FAILED");
    JsonNode executions = launch.path("statistics").path("executions");
    assertThat(executions.path("total").asInt()).as("Total executions").isEqualTo(3);
    assertThat(executions.path("passed").asInt()).as("Passed executions").isEqualTo(1);
    assertThat(executions.path("failed").asInt()).as("Failed executions").isEqualTo(1);
    assertThat(executions.path("skipped").asInt()).as("Skipped executions").isEqualTo(1);

    Map<String, JsonNode> steps = stepsByName(launch.path("id").asLong());
    assertThat(steps).containsOnlyKeys("passingTest", "failingTest", "skippedTest");
    assertThat(steps.get("passingTest").path("status").asText()).isEqualTo("PASSED");
    assertThat(steps.get("failingTest").path("status").asText()).isEqualTo("FAILED");
    assertThat(steps.get("skippedTest").path("status").asText()).isEqualTo("SKIPPED");
    assertThat(steps.get("passingTest").path("description").asText())
        .as("ParamOverrideTestNgService should add the TMS link and description")
        .contains("**Test Case ID:** [RP_E2E_SAMPLE]")
        .contains("**Description:** Sample passing test");
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

  private static Map<String, JsonNode> stepsByName(long launchId) {
    JsonPojo steps = json(API.getSteps(PROJECT, launchId));
    Map<String, JsonNode> byName = new HashMap<>();
    steps.json().path("content").forEach(step -> byName.put(step.path("name").asText(), step));
    return byName;
  }
}
