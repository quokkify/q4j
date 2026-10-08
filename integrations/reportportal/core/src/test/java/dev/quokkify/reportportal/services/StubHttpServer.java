package dev.quokkify.reportportal.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Local HTTP server shared by parallel tests: each test stubs its own unique raw path.
 */
final class StubHttpServer implements AutoCloseable {

  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final Map<String, Route> routes = new ConcurrentHashMap<>();
  private final HttpServer server;

  StubHttpServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.setExecutor(executor);
    server.createContext("/", this::dispatch);
    server.start();
  }

  String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  Route stub(String rawPath, int status, String body) {
    return stub(rawPath, status, body, Duration.ZERO);
  }

  Route stub(String rawPath, int status, String body, Duration delay) {
    Route route = new Route(status, body, delay);
    routes.put(rawPath, route);
    return route;
  }

  @Override
  public void close() {
    server.stop(0);
    executor.shutdownNow();
  }

  private void dispatch(HttpExchange exchange) throws IOException {
    String rawPath = exchange.getRequestURI().getRawPath();
    Route route = routes.get(rawPath);
    if (route == null) {
      respond(exchange, 501, "No stub for " + rawPath);
      return;
    }
    route.record(exchange);
    sleep(route.delay);
    respond(exchange, route.status, route.body);
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

  static final class Route {

    private final int status;
    private final String body;
    private final Duration delay;
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<Headers> headers = new AtomicReference<>();

    private Route(int status, String body, Duration delay) {
      this.status = status;
      this.body = body;
      this.delay = delay;
    }

    int hits() {
      return hits.get();
    }

    String method() {
      return method.get();
    }

    List<String> header(String name) {
      Headers recorded = headers.get();
      return recorded == null ? List.of() : recorded.getOrDefault(name, List.of());
    }

    private void record(HttpExchange exchange) {
      hits.incrementAndGet();
      method.set(exchange.getRequestMethod());
      headers.set(exchange.getRequestHeaders());
    }
  }
}
