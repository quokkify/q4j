package dev.quokkify.service;

import dev.quokkify.model.ReqresUserPojo;
import dev.quokkify.util.JsonConverter;

import io.restassured.response.ValidatableResponse;

public class MockApiService extends SetupApiService {

  private static final String FACT = "/fact";
  private static final String USERS = "/users";

  public ValidatableResponse getFact() {
    return get(setup(), FACT);
  }

  public ValidatableResponse createUser(ReqresUserPojo.Request requestPojo) {
    return post(setup(), USERS, JsonConverter.toJson(requestPojo));
  }
}
