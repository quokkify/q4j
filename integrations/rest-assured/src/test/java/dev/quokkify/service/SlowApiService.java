package dev.quokkify.service;

import java.time.Duration;
import java.util.List;

import dev.quokkify.filter.CustomApiTestLogFilter;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

public class SlowApiService extends ApiService {

  private final String baseUrl;
  private final int timeoutMs;

  public SlowApiService(String baseUrl, int timeoutMs) {
    this.baseUrl = baseUrl;
    this.timeoutMs = timeoutMs;
  }

  public ValidatableResponse getResource(String path) {
    return get(buildSpec(), path);
  }

  private RequestSpecification buildSpec() {
    return new RequestSpecBuilder()
        .setBaseUri(baseUrl)
        .setContentType(ContentType.JSON)
        .setConfig(RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .readTimeout(Duration.ofMillis(timeoutMs))
                .connectTimeout(Duration.ofMillis(timeoutMs))))
        .build()
        .filters(List.of(new CustomApiTestLogFilter(), new AllureRestAssured()));
  }
}
