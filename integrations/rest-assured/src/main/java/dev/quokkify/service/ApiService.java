package dev.quokkify.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.config.Configuration;
import dev.quokkify.filter.CustomApiTestLogFilter;
import dev.quokkify.model.FileParams;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;

/**
 * Common api service for all specific services.
 */
public class ApiService {

  private static final Configuration CONFIG = ConfigRegistry.get(Configuration.class);

  private static final String METHOD = "_method";

  /**
   * POST request.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse post(RequestSpecification requestSpecification, String path) {
    return requestSpecification
        .when()
        .post(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * POST request.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @param body                 request body as {@link String}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse post(RequestSpecification requestSpecification, String path, String body) {
    return requestSpecification
        .given()
        .body(body)
        .when()
        .post(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * POST request.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @param headers              request headers as {@link Map}
   * @param body                 request body as {@link String}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse post(RequestSpecification requestSpecification, String path,
                                     Map<String, String> headers, String body) {
    return requestSpecification
        .given()
        .headers(headers)
        .body(body)
        .when()
        .post(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * POST request with query parameters.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @param queryParameters      request query parameters as {@link Map}
   * @param body                 request body as {@link String}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse postWithQueryParams(RequestSpecification requestSpecification, String path,
                                                    Map<String, String> queryParameters, String body) {
    return requestSpecification
        .given()
        .queryParams(queryParameters)
        .body(body)
        .when()
        .post(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * POST request with query parameters.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @param queryParameters      request query parameters as {@link Map}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse postWithQueryParams(RequestSpecification requestSpecification, String path,
                                                    Map<String, String> queryParameters) {
    return requestSpecification
        .given()
        .queryParams(queryParameters)
        .when()
        .post(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * POST request with query parameter.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param queryParameter       request query parameter as {@link String}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse postWithQueryParams(RequestSpecification requestSpecification, String queryParameter) {
    return requestSpecification
        .given()
        .queryParam(queryParameter)
        .when()
        .post()
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * POST request with query parameter.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param queryParameter       request query parameter as {@link String}
   * @param body                 request body as {@link String}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse postWithQueryParams(RequestSpecification requestSpecification, String queryParameter,
                                                    String body) {
    return requestSpecification
        .given()
        .queryParam(queryParameter)
        .body(body)
        .when()
        .post()
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * POST form request.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param method               method name
   * @param path                 endpoint path as {@link String}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse postForm(RequestSpecification requestSpecification, UriMethod method, String path) {
    return requestSpecification
        .given()
        .param(METHOD, method.lowerCase())
        .when()
        .post(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * POST form request with parameters.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @param parameters           request parameters as {@link Map}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse postForm(RequestSpecification requestSpecification, String path,
                                         Map<String, ?> parameters) {
    return requestSpecification
        .given()
        .params(parameters)
        .when()
        .post(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * POST form request with parameters.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param method               method name
   * @param path                 endpoint path as {@link String}
   * @param parameters           request parameters as {@link Map}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse postForm(RequestSpecification requestSpecification, UriMethod method, String path,
                                         Map<String, ?> parameters) {
    return requestSpecification
        .given()
        .param(METHOD, method.lowerCase())
        .params(parameters)
        .when()
        .post(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * POST form request with query parameters.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param method               method name
   * @param path                 endpoint path as {@link String}
   * @param queryParameters      request query parameters as {@link Map}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse postFormWithQueryParams(RequestSpecification requestSpecification, UriMethod method,
                                                        String path, Map<String, ?> queryParameters) {
    return requestSpecification
        .given()
        .param(METHOD, method.lowerCase())
        .queryParams(queryParameters)
        .when()
        .post(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * POST file in request.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @param fileParams           request file params as {@link FileParams}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse postFile(RequestSpecification requestSpecification, String path,
                                         FileParams fileParams) {
    return requestSpecification
        .given()
        .params(fileParams.getParameters())
        .when()
        .multiPart(fileParams.getFileParamName(), fileParams.getFile(), fileParams.getFileContentType())
        .post(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * GET request.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse get(RequestSpecification requestSpecification, String path) {
    return requestSpecification
        .when()
        .get(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * GET request with query parameters.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param queryParameters      request query parameters as {@link Map}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse get(RequestSpecification requestSpecification, Map<String, ?> queryParameters) {
    return requestSpecification
        .given()
        .queryParams(queryParameters)
        .when()
        .get()
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * GET request with query parameters.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @param queryParameters      request query parameters as {@link Map}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse get(RequestSpecification requestSpecification, String path,
                                    Map<String, ?> queryParameters) {
    return requestSpecification
        .given()
        .queryParams(queryParameters)
        .when()
        .get(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * PATCH request.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @param body                 request body as {@link String}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse patch(RequestSpecification requestSpecification, String path, String body) {
    return requestSpecification
        .given()
        .body(body)
        .when()
        .patch(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * DELETE request.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse delete(RequestSpecification requestSpecification, String path) {
    return requestSpecification
        .when()
        .delete(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * PUT request.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @param body                 request body as {@link String}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse put(RequestSpecification requestSpecification, String path, String body) {
    return requestSpecification
        .given()
        .body(body)
        .when()
        .put(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * PUT request.
   *
   * @param requestSpecification {@link RequestSpecification}
   * @param path                 endpoint path as {@link String}
   * @param parameters           request parameters as {@link Map}
   * @return response as {@link ValidatableResponse}
   */
  protected ValidatableResponse put(RequestSpecification requestSpecification, String path, Map<String, ?> parameters) {
    return requestSpecification
        .given()
        .params(parameters)
        .when()
        .put(path)
        .then()
        .spec(getResponseSpecification());
  }

  /**
   * Get request specification with common api filters.
   *
   * @param uri         base uri
   * @param contentType content type of the request
   * @return request specification as {@link RequestSpecification}
   */
  protected RequestSpecification getRequestSpecification(String uri, ContentType contentType) {
    return getRequestSpecification(uri, contentType, List.of(new CustomApiTestLogFilter(), new AllureRestAssured()));
  }

  /**
   * Get request specification.
   *
   * @param uri         base uri
   * @param contentType content type of the request
   * @param filters     request filters as {@link List}
   * @return request specification as {@link RequestSpecification}
   */
  protected RequestSpecification getRequestSpecification(String uri, ContentType contentType, List<Filter> filters) {
    int timeoutMs = (int) (CONFIG.maxResponseTimeSeconds() * 1000L);
    return new RequestSpecBuilder()
        .setBaseUri(uri)
        .setContentType(contentType)
        .setConfig(RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.socket.timeout", timeoutMs)
                .setParam("http.connection.timeout", timeoutMs)))
        .build()
        .filters(filters);
  }

  /**
   * Get response specification, max response time `MAX_RESPONSE_TIME_SECONDS`.
   *
   * @return response specification as {@link ResponseSpecification}
   * @see Configuration#maxResponseTimeSeconds
   */
  protected ResponseSpecification getResponseSpecification() {
    return new ResponseSpecBuilder()
        .expectResponseTime(Matchers.lessThan(CONFIG.maxResponseTimeSeconds()), TimeUnit.SECONDS)
        .build();
  }
}
