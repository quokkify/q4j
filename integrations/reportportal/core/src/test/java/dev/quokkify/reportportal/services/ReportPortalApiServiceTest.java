package dev.quokkify.reportportal.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import dev.quokkify.reportportal.model.ReportPortalItem;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReportPortalApiServiceTest {

  private static final String ITEM_JSON = "{\"id\":12,\"launchId\":34,\"path\":\"suite.test\"}";

  private HttpServer server;
  private String baseUrl;
  private ReportPortalApiService service;

  @BeforeMethod
  public void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    service = new ReportPortalApiService(baseUrl + "/", "test-token");
  }

  @AfterMethod
  public void tearDown() {
    server.stop(0);
  }

  @Test
  public void getItemByUuid_sendsBearerTokenAndDeserializesResponse() {
    AtomicReference<String> method = new AtomicReference<>();
    AtomicReference<String> authorization = new AtomicReference<>();
    server.createContext("/api/v1/project/item/uuid/item-uuid", exchange -> {
      method.set(exchange.getRequestMethod());
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      respond(exchange, 200, ITEM_JSON);
    });
    server.start();

    ReportPortalItem item = service.getItemByUuid("project", "item-uuid");

    assertThat(item).isEqualTo(new ReportPortalItem(12L, 34L, "suite.test"));
    assertThat(method).hasValue("GET");
    assertThat(authorization).hasValue("Bearer test-token");
  }

  @Test
  public void getItemByUuid_sendsSingleAcceptHeader() {
    AtomicReference<Object> accept = new AtomicReference<>();
    server.createContext("/api/v1/project/item/uuid/item-uuid", exchange -> {
      accept.set(exchange.getRequestHeaders().get("Accept"));
      respond(exchange, 200, ITEM_JSON);
    });
    server.start();

    service.getItemByUuid("project", "item-uuid");

    assertThat(accept).hasValue(List.of("application/json"));
  }

  @Test
  public void getItemByUuid_returnsEmptyItemWhenNotFound() {
    server.createContext("/api/v1/project/item/uuid/missing", exchange -> respond(exchange, 404, "{\"errorCode\":4041}"));
    server.start();

    ReportPortalItem item = service.getItemByUuid("project", "missing");

    assertThat(item).isEqualTo(new ReportPortalItem(null, null, null));
  }

  @Test
  public void getItemByUuid_mapsHttpFailureWithEndpointContext() {
    server.createContext("/api/v1/project/item/uuid/missing", exchange -> respond(exchange, 503, "unavailable"));
    server.start();

    assertThatThrownBy(() -> service.getItemByUuid("project", "missing"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("HTTP request failed: GET /api/v1/project/item/uuid/missing")
        .hasCauseInstanceOf(FeignException.ServiceUnavailable.class);
  }

  @Test
  public void getItemByUuid_throwsOnUnauthorizedInsteadOfReturningEmptyItem() {
    server.createContext("/api/v1/project/item/uuid/item-uuid", exchange -> respond(exchange, 401, "{\"error\":\"unauthorized\"}"));
    server.start();

    assertThatThrownBy(() -> service.getItemByUuid("project", "item-uuid"))
        .hasMessage("HTTP request failed: GET /api/v1/project/item/uuid/item-uuid")
        .hasCauseInstanceOf(FeignException.Unauthorized.class);
  }

  @Test
  public void getItemByUuid_doesNotRetryServerErrors() {
    AtomicInteger hits = new AtomicInteger();
    server.createContext("/api/v1/project/item/uuid/flaky", exchange -> {
      hits.incrementAndGet();
      respond(exchange, 503, "unavailable");
    });
    server.start();

    assertThatThrownBy(() -> service.getItemByUuid("project", "flaky")).isInstanceOf(RuntimeException.class);

    assertThat(hits).hasValue(1);
  }

  @Test
  public void getItemByUuid_failsOnReadTimeoutWithoutRetry() {
    AtomicInteger hits = new AtomicInteger();
    server.createContext("/api/v1/project/item/uuid/slow", exchange -> {
      hits.incrementAndGet();
      sleep(Duration.ofSeconds(2));
      respond(exchange, 200, ITEM_JSON);
    });
    server.start();
    ReportPortalApiService fastTimeoutService = new ReportPortalApiService(baseUrl, "test-token",
        new Request.Options(Duration.ofSeconds(1), Duration.ofMillis(200), true));

    assertThatThrownBy(() -> fastTimeoutService.getItemByUuid("project", "slow"))
        .hasMessage("HTTP request failed: GET /api/v1/project/item/uuid/slow")
        .cause().isInstanceOf(RetryableException.class)
        .cause().isInstanceOf(SocketTimeoutException.class);
    assertThat(hits).hasValue(1);
  }

  @Test
  public void getItemByUuid_encodesPathParametersAndNormalizesEndpoint() {
    AtomicReference<String> rawPath = new AtomicReference<>();
    server.createContext("/api/v1/", exchange -> {
      rawPath.set(exchange.getRequestURI().getRawPath());
      respond(exchange, 200, ITEM_JSON);
    });
    server.start();
    ReportPortalApiService slashedEndpointService = new ReportPortalApiService(baseUrl + "///", "test-token");

    slashedEndpointService.getItemByUuid("team a", "item-uuid");

    assertThat(rawPath).hasValue("/api/v1/team%20a/item/uuid/item-uuid");
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
