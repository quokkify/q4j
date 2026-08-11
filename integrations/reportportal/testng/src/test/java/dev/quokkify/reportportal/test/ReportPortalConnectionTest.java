package dev.quokkify.reportportal.test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import dev.quokkify.model.JsonPojo;
import dev.quokkify.reportportal.config.ReportPortalConnectionConfig;

import io.qameta.allure.TmsLink;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReportPortalConnectionTest {

  private static final Logger LOG = LoggerFactory.getLogger(ReportPortalConnectionTest.class);

  private static final byte[] MINIMAL_PNG = Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");

  @TmsLink("RP_CONN_1")
  @Test(description = "Verify ReportPortal endpoint and token can access project list")
  public void shouldConnectToReportPortalApi() {
    JsonPojo result = new JsonPojo(
        buildSpec()
            .queryParam("page.page", 1)
            .queryParam("page.size", 1)
            .when().get("/api/v1/project/list")
            .then().extract().asString());

    assertThat(result.json().has("content"))
        .as("Project list response should contain 'content' key")
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

      JsonPath jsonPath = buildSpec()
          .body(logBody)
          .when().post("/api/v1/" + ReportPortalConnectionConfig.PROJECT_NAME + "/log")
          .then().extract().jsonPath();

      assertThat(jsonPath.getString("id"))
          .as("First log entry should contain an ID")
          .isNotBlank();
    } finally {
      finishTestLaunch(launchUuid);
    }
  }

  @TmsLink("RP_LOG_2")
  @Test(description = "Verify text file attachment can be sent to ReportPortal")
  public void shouldAttachTxtFileToReportPortal() {
    String launchUuid = startTestLaunch();
    try {
      int statusCode = sendMultipartLog(launchUuid,
          "Integration test: file attachment",
          "Integration test file attachment content.\n".getBytes(StandardCharsets.UTF_8),
          "test-attachment.txt", "text/plain");

      assertThat(statusCode)
          .as("File attachment should return 2xx")
          .isBetween(200, 299);
    } finally {
      finishTestLaunch(launchUuid);
    }
  }

  @TmsLink("RP_LOG_3")
  @Test(description = "Verify screenshot (PNG) attachment can be sent to ReportPortal")
  public void shouldAttachPngScreenshotToReportPortal() {
    String launchUuid = startTestLaunch();
    try {
      int statusCode = sendMultipartLog(launchUuid,
          "Integration test: screenshot attachment",
          MINIMAL_PNG, "screenshot.png", "image/png");

      assertThat(statusCode)
          .as("Screenshot attachment should return 2xx")
          .isBetween(200, 299);
    } finally {
      finishTestLaunch(launchUuid);
    }
  }

  @TmsLink("RP_NEG_1")
  @Test(description = "Verify ReportPortal rejects requests with an invalid API token")
  public void shouldRejectRequestWithInvalidToken() {
    int statusCode = RestAssured.given()
        .baseUri(ReportPortalConnectionConfig.ENDPOINT)
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer INVALID_TOKEN_VALUE_XYZ")
        .queryParam("page.page", 1)
        .queryParam("page.size", 1)
        .when().get("/api/v1/project/list")
        .then().extract().statusCode();

    assertThat(statusCode)
        .as("Invalid token should be rejected with 401 or 403")
        .isIn(401, 403);
  }

  @TmsLink("RP_NEG_2")
  @Test(description = "Verify connection failure is raised for an unreachable ReportPortal endpoint")
  public void shouldRaiseErrorForUnreachableEndpoint() {
    assertThatThrownBy(() ->
        RestAssured.given()
            .baseUri("http://localhost:19999")
            .when().get("/api/v1/project/list"))
        .as("Expected an exception for an unreachable endpoint")
        .isInstanceOf(Exception.class);
  }

  private static RequestSpecification buildSpec() {
    return RestAssured.given()
        .baseUri(ReportPortalConnectionConfig.ENDPOINT)
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer " + ReportPortalConnectionConfig.API_KEY);
  }

  private static String startTestLaunch() {
    String body = new JsonPojo()
        .setField("name", "test-coverage-run")
        .setField("startTime", Instant.now().toString())
        .setField("mode", "DEBUG")
        .asJson();

    JsonPojo response = new JsonPojo(
        buildSpec()
            .body(body)
            .when().post("/api/v1/" + ReportPortalConnectionConfig.PROJECT_NAME + "/launch")
            .then().extract().asString());

    assertThat(response.json().has("id"))
        .as("Start launch response should contain 'id'")
        .isTrue();
    return response.json().get("id").asText();
  }

  private static void finishTestLaunch(String launchUuid) {
    try {
      buildSpec()
          .body(new JsonPojo()
              .setField("endTime", Instant.now().toString())
              .setField("status", "PASSED")
              .asJson())
          .when().put("/api/v1/" + ReportPortalConnectionConfig.PROJECT_NAME + "/launch/" + launchUuid + "/finish");
    } catch (Exception e) {
      LOG.debug("Failed to finish test launch {}: {}", launchUuid, e.getMessage());
    }
  }

  private static int sendMultipartLog(String launchUuid, String message,
      byte[] fileBytes, String fileName, String fileContentType) {
    String jsonPart = new JsonPojo()
        .setField("launchUuid", launchUuid)
        .setField("time", Instant.now().toString())
        .setField("level", "INFO")
        .setField("message", message)
        .asJsonArray();

    return RestAssured.given()
        .baseUri(ReportPortalConnectionConfig.ENDPOINT)
        .header("Authorization", "Bearer " + ReportPortalConnectionConfig.API_KEY)
        .multiPart("json_request_part", jsonPart, "application/json")
        .multiPart("file", fileName, fileBytes, fileContentType)
        .when().post("/api/v1/" + ReportPortalConnectionConfig.PROJECT_NAME + "/log")
        .then().extract().statusCode();
  }
}
