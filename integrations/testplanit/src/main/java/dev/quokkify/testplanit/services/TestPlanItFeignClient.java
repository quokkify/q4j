package dev.quokkify.testplanit.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import dev.quokkify.testplanit.models.ResultSubmission;
import dev.quokkify.testplanit.models.RunCase;
import dev.quokkify.testplanit.models.TestPlanItRun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.FeignException;
import feign.RequestInterceptor;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import org.apache.commons.lang3.StringUtils;

/**
 * {@link TestPlanItClient} over the TestPlanIt REST API with Bearer token authentication.
 */
public class TestPlanItFeignClient implements TestPlanItClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String DATA = "data";
  private static final String ID = "id";
  private static final String WHERE = "where";
  private static final String CONNECT = "connect";
  private static final String RUNS_SCOPE = "RUNS";
  private static final String AUTOMATION_SCOPE = "Automation";

  private final TestPlanItFeignApi api;

  public TestPlanItFeignClient(String baseUrl, String apiToken) {
    if (StringUtils.isAnyBlank(baseUrl, apiToken)) {
      throw new IllegalArgumentException("TestPlanIt API URL and API token must be set");
    }
    this.api = Feign.builder()
        .client(new OkHttpClient())
        .encoder(new JacksonEncoder(MAPPER))
        .decoder(new JacksonDecoder(MAPPER))
        .requestInterceptor(defaultHeadersInterceptor(apiToken))
        .target(TestPlanItFeignApi.class, StringUtils.removeEnd(baseUrl.trim(), "/"));
  }

  TestPlanItFeignClient(TestPlanItFeignApi api) {
    this.api = api;
  }

  @Override
  public Optional<TestPlanItRun> findRun(int runId) {
    JsonNode run = read("testRuns", "findUnique", Map.of(
        WHERE, Map.of(ID, runId),
        "select", Map.of(ID, true, "projectId", true, "name", true, "isCompleted", true, "isDeleted", true)));
    if (isMissing(run) || run.path("isDeleted").asBoolean(false)) {
      return Optional.empty();
    }
    return Optional.of(toRun(run));
  }

  @Override
  public TestPlanItRun createRun(int projectId, String name, Integer configId, Integer milestoneId) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("name", name);
    data.put("testRunType", "REGULAR");
    data.put("project", connect(projectId));
    data.put("state", connect(findRunStateId(projectId, "IN_PROGRESS")));
    if (Objects.nonNull(configId)) {
      data.put("configuration", connect(configId));
    }
    if (Objects.nonNull(milestoneId)) {
      data.put("milestone", connect(milestoneId));
    }
    return toRun(write("testRuns", "create", Map.of(DATA, data)));
  }

  @Override
  public Map<Integer, Integer> findCaseVersions(int projectId, Collection<Integer> caseIds) {
    if (caseIds.isEmpty()) {
      return Map.of();
    }
    JsonNode cases = read("repositoryCases", "findMany", Map.of(
        WHERE, Map.of("projectId", projectId, "isDeleted", false, ID, Map.of("in", List.copyOf(caseIds))),
        "select", Map.of(ID, true, "currentVersion", true)));
    Map<Integer, Integer> versions = new HashMap<>();
    cases.forEach(repositoryCase ->
        versions.put(repositoryCase.path(ID).asInt(), repositoryCase.path("currentVersion").asInt(1)));
    return versions;
  }

  @Override
  public RunCase addCaseToRun(int runId, int caseId, int caseVersion) {
    JsonNode runCase = write("testRunCases", "upsert", Map.of(
        WHERE, Map.of("testRunId_repositoryCaseId", Map.of("testRunId", runId, "repositoryCaseId", caseId)),
        "update", Map.of(),
        "create", Map.of("testRun", connect(runId), "repositoryCase", connect(caseId))));
    int runCaseId = runCase.path(ID).asInt();
    JsonNode existingResults = read("testRunResults", "count", Map.of(
        WHERE, Map.of("testRunCaseId", runCaseId, "isDeleted", false)));
    return new RunCase(runCaseId, caseId, caseVersion, existingResults.asInt());
  }

  @Override
  public Map<String, Integer> findAutomationStatusIds(int projectId) {
    JsonNode statuses = read("status", "findMany", Map.of(
        WHERE, Map.of(
            "isEnabled", true,
            "isDeleted", false,
            "projects", Map.of("some", Map.of("projectId", projectId)),
            "scope", Map.of("some", Map.of("scope", Map.of("name", AUTOMATION_SCOPE)))),
        "select", Map.of(ID, true, "systemName", true)));
    Map<String, Integer> ids = new HashMap<>();
    statuses.forEach(status -> ids.put(status.path("systemName").asText(), status.path(ID).asInt()));
    return ids;
  }

  @Override
  public void submitResult(ResultSubmission result) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("testRunId", result.runId());
    body.put("testRunCaseId", result.runCaseId());
    body.put("statusId", result.statusId());
    body.put("attempt", result.attempt());
    body.put("testRunCaseVersion", result.caseVersion());
    if (Objects.nonNull(result.elapsedSeconds())) {
      body.put("elapsed", result.elapsedSeconds());
    }
    if (StringUtils.isNotEmpty(result.notes())) {
      body.put("notes", toRichText(result.notes()));
    }
    execute("POST /api/test-runs/submit-result", () -> api.submitResult(MAPPER.valueToTree(body)));
  }

  @Override
  public void completeRun(int runId, int projectId) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("isCompleted", true);
    findOptionalRunStateId(projectId, "DONE").ifPresent(stateId -> data.put("state", connect(stateId)));
    execute("PATCH /api/model/testRuns/update",
        () -> api.update("testRuns", MAPPER.valueToTree(Map.of(WHERE, Map.of(ID, runId), DATA, data))));
  }

  /**
   * Converts plain text to the rich-text document the TestPlanIt UI renders: one paragraph per line.
   */
  static Map<String, Object> toRichText(String text) {
    List<Map<String, Object>> paragraphs = text.lines()
        .map(line -> line.isEmpty()
            ? Map.<String, Object>of("type", "paragraph")
            : Map.<String, Object>of("type", "paragraph", "content", List.of(Map.of("type", "text", "text", line))))
        .toList();
    return Map.of("type", "doc", "content", paragraphs);
  }

  private int findRunStateId(int projectId, String workflowType) {
    return findOptionalRunStateId(projectId, workflowType)
        .or(() -> findOptionalRunStateId(projectId, null))
        .orElseThrow(() -> new IllegalStateException(
            "No workflow state for test runs found in TestPlanIt project " + projectId));
  }

  private Optional<Integer> findOptionalRunStateId(int projectId, String workflowType) {
    Map<String, Object> where = new LinkedHashMap<>();
    where.put("isEnabled", true);
    where.put("isDeleted", false);
    where.put("scope", RUNS_SCOPE);
    where.put("projects", Map.of("some", Map.of("projectId", projectId)));
    if (Objects.nonNull(workflowType)) {
      where.put("workflowType", workflowType);
    }
    JsonNode states = read("workflows", "findMany", Map.of(
        WHERE, where,
        "orderBy", Map.of("order", "asc"),
        "take", 1,
        "select", Map.of(ID, true)));
    return states.isEmpty() ? Optional.empty() : Optional.of(states.get(0).path(ID).asInt());
  }

  private JsonNode read(String model, String operation, Map<String, Object> query) {
    return data(execute("GET /api/model/%s/%s".formatted(model, operation),
        () -> api.read(model, operation, toJson(query))));
  }

  private JsonNode write(String model, String operation, Map<String, Object> body) {
    return data(execute("POST /api/model/%s/%s".formatted(model, operation),
        () -> api.write(model, operation, MAPPER.valueToTree(body))));
  }

  private static JsonNode data(JsonNode response) {
    return Objects.isNull(response) ? MAPPER.missingNode() : response.path(DATA);
  }

  private static boolean isMissing(JsonNode node) {
    return node.isMissingNode() || node.isNull();
  }

  private static TestPlanItRun toRun(JsonNode run) {
    return new TestPlanItRun(
        run.path(ID).asInt(),
        run.path("projectId").asInt(),
        run.path("name").asText(),
        run.path("isCompleted").asBoolean(false));
  }

  private static Map<String, Object> connect(int id) {
    return Map.of(CONNECT, Map.of(ID, id));
  }

  private static String toJson(Map<String, Object> value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Can not serialize TestPlanIt query", e);
    }
  }

  private static RequestInterceptor defaultHeadersInterceptor(String apiToken) {
    return template -> {
      template.header("Content-Type", "application/json");
      template.header("Accept", "application/json");
      template.header("Authorization", "Bearer " + apiToken);
    };
  }

  private static JsonNode execute(String endpoint, Supplier<JsonNode> request) {
    try {
      return request.get();
    } catch (FeignException e) {
      throw new TestPlanItException(
          "TestPlanIt request failed: %s, status %d, response: %s"
              .formatted(endpoint, e.status(), e.contentUTF8()), e);
    }
  }
}
