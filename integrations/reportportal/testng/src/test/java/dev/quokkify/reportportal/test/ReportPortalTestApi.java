package dev.quokkify.reportportal.test;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import feign.Retryer;
import feign.okhttp.OkHttpClient;

interface ReportPortalTestApi {

  @RequestLine("GET /api/v1/project/list?page.page=1&page.size=1")
  Response getProjects();

  @RequestLine("POST /api/v1/{project}/launch")
  @Headers("Content-Type: application/json")
  Response startLaunch(@Param("project") String project, String body);

  @RequestLine("PUT /api/v1/{project}/launch/{launchUuid}/finish")
  @Headers("Content-Type: application/json")
  Response finishLaunch(@Param("project") String project, @Param("launchUuid") String launchUuid, String body);

  @RequestLine("GET /api/v1/{project}/launch/uuid/{launchUuid}")
  Response getLaunch(@Param("project") String project, @Param("launchUuid") String launchUuid);

  @RequestLine("GET /api/v1/{project}/launch?filter.eq.name={name}&page.size=10")
  Response findLaunches(@Param("project") String project, @Param("name") String name);

  @RequestLine("POST /api/v1/{project}/item")
  @Headers("Content-Type: application/json")
  Response startItem(@Param("project") String project, String body);

  @RequestLine("PUT /api/v1/{project}/item/{itemUuid}")
  @Headers("Content-Type: application/json")
  Response finishItem(@Param("project") String project, @Param("itemUuid") String itemUuid, String body);

  @RequestLine("GET /api/v1/{project}/item?filter.eq.launchId={launchId}&filter.eq.type=STEP&page.size=50")
  Response getSteps(@Param("project") String project, @Param("launchId") long launchId);

  @RequestLine("POST /api/v1/{project}/log")
  @Headers("Content-Type: application/json")
  Response sendLog(@Param("project") String project, String body);

  @RequestLine("GET /api/v1/{project}/log/uuid/{logUuid}")
  Response getLog(@Param("project") String project, @Param("logUuid") String logUuid);

  @RequestLine("POST /api/v1/{project}/log")
  @Headers("Content-Type: multipart/form-data; boundary={boundary}")
  Response sendMultipartLog(@Param("project") String project, @Param("boundary") String boundary, byte[] body);

  static ReportPortalTestApi create(String endpoint, String apiKey) {
    return Feign.builder()
        .client(new OkHttpClient())
        .retryer(Retryer.NEVER_RETRY)
        .requestInterceptor(template -> template.header("Authorization", "Bearer " + apiKey))
        .target(ReportPortalTestApi.class, endpoint.replaceFirst("/+$", ""));
  }
}
