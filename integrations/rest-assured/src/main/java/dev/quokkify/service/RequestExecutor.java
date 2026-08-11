package dev.quokkify.service;

import dev.quokkify.filter.CustomApiTestLogFilter;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/**
 * Request executor.
 */
public class RequestExecutor extends ApiService {

  /**
   * GET request.
   *
   * @param baseUrl base url
   * @return {@link ValidatableResponse}
   */
  public ValidatableResponse get(String baseUrl) {
    return get(setup(), baseUrl);
  }

  /**
   * Setup common request specification.
   *
   * @return request specification as {@link RequestSpecification}
   */
  private RequestSpecification setup() {
    return RestAssured
        .given()
        .filters(new CustomApiTestLogFilter(), new AllureRestAssured());
  }
}
