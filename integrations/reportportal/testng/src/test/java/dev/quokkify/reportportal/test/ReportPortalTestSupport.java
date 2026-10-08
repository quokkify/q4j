package dev.quokkify.reportportal.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import dev.quokkify.model.JsonPojo;
import dev.quokkify.reportportal.config.ReportPortalConnectionConfig;

import feign.Response;
import feign.Util;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

final class ReportPortalTestSupport {

  static final String PROJECT = ReportPortalConnectionConfig.PROJECT_NAME;
  static final ReportPortalTestApi API =
      ReportPortalTestApi.create(ReportPortalConnectionConfig.ENDPOINT, ReportPortalConnectionConfig.API_KEY);

  private static final Logger LOG = LoggerFactory.getLogger(ReportPortalTestSupport.class);
  private static final Duration CLEANUP_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration CLEANUP_POLL_INTERVAL = Duration.ofMillis(500);

  private ReportPortalTestSupport() {
  }

  static JsonPojo json(Response response) {
    return new JsonPojo(bodyOf(response));
  }

  static String bodyOf(Response response) {
    try (response) {
      return Util.toString(response.body().asReader(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static String startLaunch(String name) {
    String body = new JsonPojo()
        .setField("name", name)
        .setField("startTime", Instant.now().toString())
        .setField("mode", "DEBUG")
        .asJson();
    return createdId(API.startLaunch(PROJECT, body), "Start launch");
  }

  static void finishAndDeleteLaunch(String launchUuid) {
    String body = new JsonPojo()
        .setField("endTime", Instant.now().toString())
        .setField("status", "PASSED")
        .asJson();
    try (Response ignored = API.finishLaunch(PROJECT, launchUuid, body)) {
      LOG.debug("Finished test launch {}", launchUuid);
      deleteLaunch(launchIdOf(launchUuid));
    } catch (Exception e) {
      LOG.warn("Failed to finish and delete test launch {}: {}", launchUuid, e.getMessage());
    }
  }

  static void deleteLaunch(long launchId) {
    Awaitility.await("launch %d deleted".formatted(launchId))
        .atMost(CLEANUP_TIMEOUT)
        .pollInterval(CLEANUP_POLL_INTERVAL)
        .until(() -> {
          try (Response response = API.deleteLaunch(PROJECT, launchId)) {
            return response.status() == 200 || response.status() == 404;
          }
        });
    LOG.debug("Deleted test launch {}", launchId);
  }

  private static long launchIdOf(String launchUuid) {
    return Awaitility.await("launch %s indexed".formatted(launchUuid))
        .atMost(CLEANUP_TIMEOUT)
        .pollInterval(CLEANUP_POLL_INTERVAL)
        .ignoreExceptions()
        .until(() -> json(API.getLaunch(PROJECT, launchUuid)).requiredAt("/id").asLong(), id -> id > 0);
  }

  static String startStep(String launchUuid, String name) {
    String body = new JsonPojo()
        .setField("launchUuid", launchUuid)
        .setField("name", name)
        .setField("type", "STEP")
        .setField("startTime", Instant.now().toString())
        .asJson();
    return createdId(API.startItem(PROJECT, body), "Start item");
  }

  static void finishStep(String launchUuid, String itemUuid) {
    String body = new JsonPojo()
        .setField("launchUuid", launchUuid)
        .setField("endTime", Instant.now().toString())
        .setField("status", "PASSED")
        .asJson();
    try (Response ignored = API.finishItem(PROJECT, itemUuid, body)) {
      LOG.debug("Finished test item {}", itemUuid);
    } catch (Exception e) {
      LOG.debug("Failed to finish test item {}: {}", itemUuid, e.getMessage());
    }
  }

  private static String createdId(Response response, String operation) {
    JsonPojo created = json(response);
    String id = created.at("/id").asText();
    assertThat(id)
        .as("%s response should contain 'id', got: %s", operation, created.asJson())
        .isNotBlank();
    return id;
  }
}
