package dev.quokkify.reportportal.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import dev.quokkify.reportportal.model.ReportPortalItem;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReportPortalApiServiceTest {

  private HttpServer server;
  private ReportPortalApiService service;

  @BeforeMethod
  public void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    service = new ReportPortalApiService("http://localhost:" + server.getAddress().getPort() + "/", "test-token");
  }

  @AfterMethod
  public void tearDown() {
    server.stop(0);
  }

  @Test
  public void getItemByUuid_sendsBearerTokenAndDeserializesResponse() {
    server.createContext("/api/v1/project/item/uuid/item-uuid", exchange -> {
      assertThat(exchange.getRequestMethod()).isEqualTo("GET");
      assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-token");
      respond(exchange, 200, "{\"id\":12,\"launchId\":34,\"path\":\"suite.test\"}");
    });
    server.start();

    ReportPortalItem item = service.getItemByUuid("project", "item-uuid");

    assertThat(item).isEqualTo(new ReportPortalItem(12L, 34L, "suite.test"));
  }

  @Test
  public void getItemByUuid_mapsHttpFailureWithEndpointContext() {
    server.createContext("/api/v1/project/item/uuid/missing", exchange -> respond(exchange, 503, "unavailable"));
    server.start();

    assertThatThrownBy(() -> service.getItemByUuid("project", "missing"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("HTTP request failed: GET /api/v1/project/item/uuid/missing")
        .hasCauseInstanceOf(RuntimeException.class);
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }
}
