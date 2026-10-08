package dev.quokkify.reportportal.test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import dev.quokkify.model.JsonPojo;
import dev.quokkify.reportportal.config.ReportPortalConnectionConfig;

import feign.Response;
import feign.RetryableException;
import io.qameta.allure.TmsLink;
import org.awaitility.Awaitility;
import org.testng.annotations.Test;

import static dev.quokkify.reportportal.test.ReportPortalTestSupport.API;
import static dev.quokkify.reportportal.test.ReportPortalTestSupport.bodyOf;
import static dev.quokkify.reportportal.test.ReportPortalTestSupport.finishLaunch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReportPortalConnectionTest {

  private static final byte[] MINIMAL_PNG = Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");

  @TmsLink("RP_CONN_1")
  @Test(description = "Verify ReportPortal endpoint and token can access project list")
  public void shouldConnectToReportPortalApi() {
    JsonPojo result = new JsonPojo(bodyOf(API.getProjects()));

    assertThat(result.at("/content").isArray())
        .as("Project list response should contain 'content' array")
        .isTrue();
  }

  @TmsLink("RP_LOG_1")
  @Test(description = "Verify text log can be sent to ReportPortal and returns log entry ID")
  public void shouldSendTextLogToReportPortal() {
    String launchUuid = startTestLaunch();
    try {
      String logBody = new JsonPojo()
          .setField("launchUuid", launchUuid)
          .setField("time", Instant.now().toString())
          .setField("level", "INFO")
          .setField("message", "Integration test: text log verification")
          .asJson();

      JsonPojo response = new JsonPojo(bodyOf(API.sendLog(ReportPortalConnectionConfig.PROJECT_NAME, logBody)));

      assertThat(response.at("/id").asText())
          .as("First log entry should contain an ID")
          .isNotBlank();
    } finally {
      finishLaunch(launchUuid);
    }
  }

  @TmsLink("RP_LOG_2")
  @Test(description = "Verify text file attachment can be sent to ReportPortal")
  public void shouldAttachTxtFileToReportPortal() {
    String launchUuid = startTestLaunch();
    try {
      String logUuid = sendMultipartLog(launchUuid,
          "Integration test: file attachment",
          "Integration test file attachment content.\n".getBytes(StandardCharsets.UTF_8),
          "test-attachment.txt", "text/plain");

      assertAttachmentStored(logUuid, "text/plain");
    } finally {
      finishLaunch(launchUuid);
    }
  }

  @TmsLink("RP_LOG_3")
  @Test(description = "Verify screenshot (PNG) attachment can be sent to ReportPortal")
  public void shouldAttachPngScreenshotToReportPortal() {
    String launchUuid = startTestLaunch();
    try {
      String logUuid = sendMultipartLog(launchUuid,
          "Integration test: screenshot attachment",
          MINIMAL_PNG, "screenshot.png", "image/png");

      assertAttachmentStored(logUuid, "image/png");
    } finally {
      finishLaunch(launchUuid);
    }
  }

  @TmsLink("RP_NEG_1")
  @Test(description = "Verify ReportPortal rejects requests with an invalid API token")
  public void shouldRejectRequestWithInvalidToken() {
    ReportPortalTestApi api =
        ReportPortalTestApi.create(ReportPortalConnectionConfig.ENDPOINT, "INVALID_TOKEN_VALUE_XYZ");
    int statusCode;
    try (Response response = api.getProjects()) {
      statusCode = response.status();
    }

    assertThat(statusCode)
        .as("Invalid token should be rejected with 401 or 403")
        .isIn(401, 403);
  }

  @TmsLink("RP_NEG_2")
  @Test(description = "Verify connection failure is raised for an unreachable ReportPortal endpoint")
  public void shouldRaiseErrorForUnreachableEndpoint() {
    ReportPortalTestApi api = ReportPortalTestApi.create("http://localhost:19999", "any");

    assertThatThrownBy(api::getProjects)
        .as("Expected a connection failure for an unreachable endpoint")
        .isInstanceOf(RetryableException.class);
  }

  private static String startTestLaunch() {
    return ReportPortalTestSupport.startLaunch("test-coverage-run");
  }

  private static String sendMultipartLog(String launchUuid, String message,
      byte[] fileBytes, String fileName, String fileContentType) {
    String jsonPart = new JsonPojo()
        .setField("launchUuid", launchUuid)
        .setField("time", Instant.now().toString())
        .setField("level", "INFO")
        .setField("message", message)
        .setField("file", new JsonPojo().setField("name", fileName))
        .asJsonArray();

    String boundary = UUID.randomUUID().toString();
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writePart(body, boundary, "name=\"json_request_part\"", "application/json",
        jsonPart.getBytes(StandardCharsets.UTF_8));
    writePart(body, boundary, "name=\"file\"; filename=\"" + fileName + "\"", fileContentType, fileBytes);
    body.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

    JsonPojo response = new JsonPojo(bodyOf(
        API.sendMultipartLog(ReportPortalConnectionConfig.PROJECT_NAME, boundary, body.toByteArray())));
    String logUuid = response.at("/responses/0/id").asText();
    assertThat(logUuid)
        .as("Multipart log response should contain created log ID, got: %s", response.asJson())
        .isNotBlank();
    return logUuid;
  }

  private static void assertAttachmentStored(String logUuid, String expectedContentType) {
    Awaitility.await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> {
          JsonPojo log = new JsonPojo(bodyOf(API.getLog(ReportPortalConnectionConfig.PROJECT_NAME, logUuid)));
          assertThat(log.at("/binaryContent/contentType").asText())
              .as("Attachment should be stored for log %s", logUuid)
              .isEqualTo(expectedContentType);
        });
  }

  private static void writePart(ByteArrayOutputStream body, String boundary, String disposition,
      String contentType, byte[] content) {
    body.writeBytes(("--" + boundary + "\r\n"
        + "Content-Disposition: form-data; " + disposition + "\r\n"
        + "Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
    body.writeBytes(content);
    body.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
  }
}
