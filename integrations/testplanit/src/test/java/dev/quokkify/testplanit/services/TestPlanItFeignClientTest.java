package dev.quokkify.testplanit.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.quokkify.testplanit.models.ResultSubmission;
import dev.quokkify.testplanit.models.RunCase;
import dev.quokkify.testplanit.models.TestPlanItRun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.qameta.allure.TmsLink;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestPlanItFeignClientTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String TOKEN = "tpi_unit_test_token";

  private HttpServer server;
  private final List<RecordedRequest> requests = new ArrayList<>();
  private final Deque<Response> responses = new ArrayDeque<>();
  private TestPlanItFeignClient client;

  @BeforeMethod
  public void startServer() throws IOException {
    requests.clear();
    responses.clear();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", this::handle);
    server.start();
    client = new TestPlanItFeignClient("http://127.0.0.1:%d/".formatted(server.getAddress().getPort()), TOKEN);
  }

  @AfterMethod(alwaysRun = true)
  public void stopServer() {
    server.stop(0);
  }

  @TmsLink("TPI_CLIENT_1")
  @Test(description = "Requests carry the Bearer token; reads send the query as JSON in q")
  public void readsWithBearerTokenAndJsonQuery() {
    respond(200, "{\"data\":{\"id\":5,\"projectId\":1,\"name\":\"Nightly\",\"isCompleted\":true,\"isDeleted\":false}}");

    TestPlanItRun run = client.findRun(5).orElseThrow();

    RecordedRequest request = requests.get(0);
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.path()).isEqualTo("/api/model/testRuns/findUnique");
    assertThat(request.authorization()).isEqualTo("Bearer " + TOKEN);
    assertThat(request.query().at("/where/id").asInt()).isEqualTo(5);
    assertThat(run).isEqualTo(new TestPlanItRun(5, 1, "Nightly", true));
  }

  @TmsLink("TPI_CLIENT_2")
  @Test(description = "A missing or deleted run is not found")
  public void missingRunIsEmpty() {
    respond(200, "{\"data\":null}");
    respond(200, "{\"data\":{\"id\":6,\"projectId\":1,\"name\":\"x\",\"isCompleted\":false,\"isDeleted\":true}}");

    assertThat(client.findRun(404)).isEmpty();
    assertThat(client.findRun(6)).isEmpty();
  }

  @TmsLink("TPI_CLIENT_3")
  @Test(description = "A run is created in the IN_PROGRESS state with relations in connect syntax")
  public void createsRunWithConnectRelations() {
    respond(200, "{\"data\":[{\"id\":7}]}");
    respond(200, "{\"data\":{\"id\":9,\"projectId\":1,\"name\":\"Nightly\",\"isCompleted\":false}}");

    TestPlanItRun run = client.createRun(1, "Nightly", 2, 3);

    assertThat(requests.get(0).query().at("/where/workflowType").asText()).isEqualTo("IN_PROGRESS");
    assertThat(requests.get(0).query().at("/where/scope").asText()).isEqualTo("RUNS");
    RecordedRequest create = requests.get(1);
    assertThat(create.method()).isEqualTo("POST");
    assertThat(create.path()).isEqualTo("/api/model/testRuns/create");
    JsonNode data = create.body().path("data");
    assertThat(data.path("name").asText()).isEqualTo("Nightly");
    assertThat(data.at("/project/connect/id").asInt()).isEqualTo(1);
    assertThat(data.at("/state/connect/id").asInt()).isEqualTo(7);
    assertThat(data.at("/configuration/connect/id").asInt()).isEqualTo(2);
    assertThat(data.at("/milestone/connect/id").asInt()).isEqualTo(3);
    assertThat(run.id()).isEqualTo(9);
  }

  @TmsLink("TPI_CLIENT_4")
  @Test(description = "A case is added with upsert on the run and case pair, and its results are counted")
  public void addsCaseWithUpsert() {
    respond(200, "{\"data\":{\"id\":42}}");
    respond(200, "{\"data\":2}");

    RunCase runCase = client.addCaseToRun(9, 6, 4);

    RecordedRequest upsert = requests.get(0);
    assertThat(upsert.path()).isEqualTo("/api/model/testRunCases/upsert");
    assertThat(upsert.body().at("/where/testRunId_repositoryCaseId/testRunId").asInt()).isEqualTo(9);
    assertThat(upsert.body().at("/where/testRunId_repositoryCaseId/repositoryCaseId").asInt()).isEqualTo(6);
    assertThat(upsert.body().at("/create/repositoryCase/connect/id").asInt()).isEqualTo(6);
    assertThat(requests.get(1).path()).isEqualTo("/api/model/testRunResults/count");
    assertThat(runCase).isEqualTo(new RunCase(42, 6, 4, 2));
  }

  @TmsLink("TPI_CLIENT_5")
  @Test(description = "Case versions and automation statuses are read from the project")
  public void readsCaseVersionsAndStatuses() {
    respond(200, "{\"data\":[{\"id\":6,\"currentVersion\":2}]}");
    respond(200, "{\"data\":[{\"id\":2,\"systemName\":\"passed\"},{\"id\":3,\"systemName\":\"failed\"}]}");

    Map<Integer, Integer> versions = client.findCaseVersions(1, Set.of(6, 999));
    Map<String, Integer> statuses = client.findAutomationStatusIds(1);

    assertThat(requests.get(0).query().at("/where/id/in")).hasSize(2);
    assertThat(versions).containsExactly(Map.entry(6, 2));
    assertThat(requests.get(1).query().at("/where/scope/some/scope/name").asText()).isEqualTo("Automation");
    assertThat(statuses).containsOnly(Map.entry("passed", 2), Map.entry("failed", 3));
  }

  @TmsLink("TPI_CLIENT_6")
  @Test(description = "A result goes to submit-result with notes as a rich-text document")
  public void submitsResult() {
    respond(200, "{\"result\":{\"id\":1}}");

    client.submitResult(new ResultSubmission(9, 42, 3, 2, 4, "line 1\n\nline 3", 5L));

    RecordedRequest request = requests.get(0);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.path()).isEqualTo("/api/test-runs/submit-result");
    JsonNode body = request.body();
    assertThat(body.path("testRunId").asInt()).isEqualTo(9);
    assertThat(body.path("testRunCaseId").asInt()).isEqualTo(42);
    assertThat(body.path("statusId").asInt()).isEqualTo(3);
    assertThat(body.path("attempt").asInt()).isEqualTo(2);
    assertThat(body.path("testRunCaseVersion").asInt()).isEqualTo(4);
    assertThat(body.path("elapsed").asLong()).isEqualTo(5L);
    assertThat(body.at("/notes/type").asText()).isEqualTo("doc");
    assertThat(body.at("/notes/content")).hasSize(3);
    assertThat(body.at("/notes/content/0/content/0/text").asText()).isEqualTo("line 1");
    assertThat(body.at("/notes/content/1/content").isMissingNode()).isTrue();
  }

  @TmsLink("TPI_CLIENT_7")
  @Test(description = "Completing a run sets isCompleted and the DONE state with PATCH")
  public void completesRun() {
    respond(200, "{\"data\":[{\"id\":9}]}");
    respond(200, "{\"data\":{\"id\":9}}");

    client.completeRun(9, 1);

    RecordedRequest update = requests.get(1);
    assertThat(update.method()).isEqualTo("PATCH");
    assertThat(update.path()).isEqualTo("/api/model/testRuns/update");
    assertThat(update.body().at("/where/id").asInt()).isEqualTo(9);
    assertThat(update.body().at("/data/isCompleted").asBoolean()).isTrue();
    assertThat(update.body().at("/data/state/connect/id").asInt()).isEqualTo(9);
  }

  @TmsLink("TPI_CLIENT_8")
  @Test(description = "An HTTP error names the endpoint and response, and never the token")
  public void wrapsHttpErrorsWithoutToken() {
    respond(409, "{\"error\":\"This test run is completed\",\"code\":\"RUN_COMPLETED\"}");

    assertThatThrownBy(() -> client.submitResult(new ResultSubmission(9, 42, 2, 1, 1, null, null)))
        .isInstanceOf(TestPlanItException.class)
        .hasMessageContaining("POST /api/test-runs/submit-result")
        .hasMessageContaining("status 409")
        .hasMessageContaining("RUN_COMPLETED")
        .hasMessageNotContaining(TOKEN);
  }

  @TmsLink("TPI_CLIENT_9")
  @Test(description = "The client is not created without URL or token")
  public void requiresUrlAndToken() {
    assertThatThrownBy(() -> new TestPlanItFeignClient("http://localhost", " "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TestPlanItFeignClient(null, TOKEN))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private void respond(int status, String body) {
    responses.add(new Response(status, body));
  }

  private void handle(HttpExchange exchange) throws IOException {
    String rawQuery = exchange.getRequestURI().getRawQuery();
    JsonNode query = rawQuery == null || !rawQuery.startsWith("q=")
        ? MAPPER.missingNode()
        : MAPPER.readTree(URLDecoder.decode(rawQuery.substring(2), StandardCharsets.UTF_8));
    JsonNode body;
    try (InputStream input = exchange.getRequestBody()) {
      byte[] bytes = input.readAllBytes();
      body = bytes.length == 0 ? MAPPER.missingNode() : MAPPER.readTree(bytes);
    }
    requests.add(new RecordedRequest(
        exchange.getRequestMethod(),
        exchange.getRequestURI().getPath(),
        exchange.getRequestHeaders().getFirst("Authorization"),
        query,
        body));
    Response response = responses.isEmpty() ? new Response(500, "{\"error\":\"unexpected\"}") : responses.poll();
    byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(response.status(), bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }

  private record RecordedRequest(String method, String path, String authorization, JsonNode query, JsonNode body) {
  }

  private record Response(int status, String body) {
  }
}
