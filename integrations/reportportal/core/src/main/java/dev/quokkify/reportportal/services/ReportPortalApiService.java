package dev.quokkify.reportportal.services;

import dev.quokkify.filter.CustomExternalServiceLogFilter;
import dev.quokkify.reportportal.configs.ReportPortalConfig;
import dev.quokkify.reportportal.model.ReportPortalItem;
import dev.quokkify.service.ApiService;
import dev.quokkify.util.JsonConverter;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;

public class ReportPortalApiService extends ApiService {

  private static final String ITEM_UUID_PATH = "/api/v1/%s/item/uuid/%s";

  public ReportPortalItem getItemByUuid(String projectName, String itemUuid) {
    ValidatableResponse response = get(buildSpec(), ITEM_UUID_PATH.formatted(projectName, itemUuid));
    return JsonConverter.fromString(response.extract().asString(), ReportPortalItem.class);
  }

  private static RequestSpecification buildSpec() {
    return RestAssured.given()
        .filter(new CustomExternalServiceLogFilter())
        .baseUri(StringUtils.stripEnd(ReportPortalConfig.RP_ENDPOINT, "/"))
        .auth().preemptive().oauth2(ReportPortalConfig.RP_API_KEY)
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON);
  }
}
