package dev.quokkify.testrail.services;

import java.util.List;
import java.util.Map;

import dev.quokkify.testrail.models.CaseFields;
import dev.quokkify.testrail.models.Sections;
import dev.quokkify.testrail.models.TestCase;
import dev.quokkify.testrail.models.TestCaseType;
import dev.quokkify.testrail.models.TestCases;
import dev.quokkify.testrail.models.TestDataList;
import dev.quokkify.testrail.models.TestPlan;
import dev.quokkify.testrail.models.TestRailUser;
import dev.quokkify.testrail.models.TestRun;
import dev.quokkify.testrail.models.TestRuns;
import dev.quokkify.testrail.models.TestSuite;

import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;

interface TestRailFeignApi {

  @RequestLine("GET /index.php?api/v2/get_user_by_email")
  TestRailUser getUserByEmail(@QueryMap Map<String, Object> query);

  @RequestLine("GET /index.php?api/v2/get_case_types")
  List<TestCaseType> getTestCaseTypes();

  @RequestLine("GET /index.php?api/v2/get_cases/{projectId}")
  TestCases getCases(@Param("projectId") int projectId, @QueryMap Map<String, Object> query);

  @RequestLine("GET /index.php?api/v2/get_case/{caseId}")
  TestCase getCase(@Param("caseId") int caseId);

  @RequestLine("GET /index.php?api/v2/get_runs/{projectId}")
  TestRuns getRuns(@Param("projectId") int projectId);

  @RequestLine("GET /index.php?api/v2/get_sections/{projectId}")
  Sections getSections(@Param("projectId") int projectId, @QueryMap Map<String, Object> query);

  @RequestLine("GET /index.php?api/v2/get_run/{runId}")
  TestRun getRun(@Param("runId") int runId);

  @RequestLine("POST /index.php?api/v2/add_run/{projectId}")
  @Headers("Content-Type: application/json")
  TestRun addRun(@Param("projectId") int projectId, Object run);

  @RequestLine("POST /index.php?api/v2/close_run/{runId}")
  @Headers("Content-Type: application/json")
  TestRun closeRun(@Param("runId") int runId, Map<String, Object> body);

  @RequestLine("GET /index.php?api/v2/get_plan/{planId}")
  TestPlan getPlan(@Param("planId") int planId);

  @RequestLine("POST /index.php?api/v2/add_result_for_case/{runId}/{caseId}")
  @Headers("Content-Type: application/json")
  void addResultForCase(@Param("runId") int runId, @Param("caseId") String caseId, Object body);

  @RequestLine("GET /index.php?api/v2/get_tests/{runId}")
  TestDataList getTests(@Param("runId") int runId);

  @RequestLine("GET /index.php?api/v2/get_suites/{projectId}")
  List<TestSuite> getSuites(@Param("projectId") int projectId);

  @RequestLine("GET /index.php?api/v2/get_suite/{suiteId}")
  TestSuite getSuite(@Param("suiteId") int suiteId);

  @RequestLine("GET /index.php?api/v2/get_case_fields")
  List<CaseFields> getCaseFields();

  @RequestLine("POST /index.php?api/v2/update_case/{caseId}")
  @Headers("Content-Type: application/json")
  void updateCase(@Param("caseId") String caseId, Object body);

  @RequestLine("POST /index.php?api/v2/delete_run/{runId}")
  @Headers("Content-Type: application/json")
  void deleteRun(@Param("runId") int runId, Map<String, Object> body);
}
