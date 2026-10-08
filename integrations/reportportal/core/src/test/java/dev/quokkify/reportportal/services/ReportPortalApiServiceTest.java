package dev.quokkify.reportportal.services;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import dev.quokkify.model.JsonPojo;
import dev.quokkify.reportportal.model.ReportPortalItem;
import dev.quokkify.reportportal.services.StubHttpServer.Route;

import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReportPortalApiServiceTest {

  private static final String PROJECT = "project";
  private static final String ITEM_JSON = new JsonPojo()
      .setField("id", 12)
      .setField("launchId", 34)
      .setField("path", "suite.test")
      .asJson();

  private StubHttpServer server;
  private ReportPortalApiService service;

  @BeforeClass
  public void startServer() throws IOException {
    server = new StubHttpServer();
    service = new ReportPortalApiService(server.baseUrl() + "/", "test-token");
  }

  @AfterClass(alwaysRun = true)
  public void stopServer() {
    if (server != null) {
      server.close();
    }
  }

  @Test
  public void getItemByUuid_sendsBearerTokenAndDeserializesResponse() {
    String uuid = uniqueUuid();
    Route route = server.stub(itemPath(PROJECT, uuid), 200, ITEM_JSON);

    ReportPortalItem item = service.getItemByUuid(PROJECT, uuid);

    assertThat(item).isEqualTo(new ReportPortalItem(12L, 34L, "suite.test"));
    assertThat(route.method()).isEqualTo("GET");
    assertThat(route.header("Authorization")).containsExactly("Bearer test-token");
  }

  @Test
  public void getItemByUuid_sendsSingleAcceptHeader() {
    String uuid = uniqueUuid();
    Route route = server.stub(itemPath(PROJECT, uuid), 200, ITEM_JSON);

    service.getItemByUuid(PROJECT, uuid);

    assertThat(route.header("Accept")).isEqualTo(List.of("application/json"));
  }

  @Test
  public void getItemByUuid_returnsEmptyItemWhenNotFound() {
    String uuid = uniqueUuid();
    server.stub(itemPath(PROJECT, uuid), 404, errorJson(4041));

    ReportPortalItem item = service.getItemByUuid(PROJECT, uuid);

    assertThat(item).isEqualTo(new ReportPortalItem(null, null, null));
  }

  @Test
  public void getItemByUuid_mapsHttpFailureWithEndpointContext() {
    String uuid = uniqueUuid();
    server.stub(itemPath(PROJECT, uuid), 503, "unavailable");

    assertThatThrownBy(() -> service.getItemByUuid(PROJECT, uuid))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("HTTP request failed: GET " + itemPath(PROJECT, uuid))
        .hasCauseInstanceOf(FeignException.ServiceUnavailable.class);
  }

  @Test
  public void getItemByUuid_throwsOnUnauthorizedInsteadOfReturningEmptyItem() {
    String uuid = uniqueUuid();
    server.stub(itemPath(PROJECT, uuid), 401, errorJson(4003));

    assertThatThrownBy(() -> service.getItemByUuid(PROJECT, uuid))
        .hasMessage("HTTP request failed: GET " + itemPath(PROJECT, uuid))
        .hasCauseInstanceOf(FeignException.Unauthorized.class);
  }

  @Test
  public void getItemByUuid_doesNotRetryServerErrors() {
    String uuid = uniqueUuid();
    Route route = server.stub(itemPath(PROJECT, uuid), 503, "unavailable");

    assertThatThrownBy(() -> service.getItemByUuid(PROJECT, uuid)).isInstanceOf(RuntimeException.class);

    assertThat(route.hits()).isEqualTo(1);
  }

  @Test
  public void getItemByUuid_failsOnReadTimeoutWithoutRetry() {
    String uuid = uniqueUuid();
    Route route = server.stub(itemPath(PROJECT, uuid), 200, ITEM_JSON, Duration.ofSeconds(2));
    ReportPortalApiService fastTimeoutService = new ReportPortalApiService(server.baseUrl(), "test-token",
        new Request.Options(Duration.ofSeconds(1), Duration.ofMillis(200), true));

    assertThatThrownBy(() -> fastTimeoutService.getItemByUuid(PROJECT, uuid))
        .hasMessage("HTTP request failed: GET " + itemPath(PROJECT, uuid))
        .cause().isInstanceOf(RetryableException.class)
        .cause().isInstanceOf(SocketTimeoutException.class);
    assertThat(route.hits()).isEqualTo(1);
  }

  @Test
  public void getItemByUuid_encodesPathParametersAndNormalizesEndpoint() {
    String uuid = uniqueUuid();
    Route route = server.stub(itemPath("team%20a", uuid), 200, ITEM_JSON);
    ReportPortalApiService slashedEndpointService = new ReportPortalApiService(server.baseUrl() + "///", "test-token");

    slashedEndpointService.getItemByUuid("team a", uuid);

    assertThat(route.hits()).as("Request must hit the encoded path").isEqualTo(1);
  }

  private static String uniqueUuid() {
    return UUID.randomUUID().toString();
  }

  private static String itemPath(String rawProject, String uuid) {
    return "/api/v1/%s/item/uuid/%s".formatted(rawProject, uuid);
  }

  private static String errorJson(int errorCode) {
    return new JsonPojo().setField("errorCode", errorCode).asJson();
  }
}
