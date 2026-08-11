package dev.quokkify.test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.constant.DateFormat;
import dev.quokkify.formatter.LocalDateTimeFormatter;
import dev.quokkify.generator.LocalDateTimeGenerator;
import dev.quokkify.helper.MockApiHelper;
import dev.quokkify.helper.ResponseHelper;
import dev.quokkify.model.CatFactPojo;
import dev.quokkify.model.ReqresUserPojo;
import dev.quokkify.parser.LocalDateTimeParser;
import dev.quokkify.step.CatFactApiSteps;

import io.qameta.allure.TmsLink;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

public class ApiTest {

  private final CatFactApiSteps catFactApiSteps = new CatFactApiSteps();

  @TmsLink("API_ID_1")
  @TestGroup("API")
  @Test(description = "Verify 'GET' method and 'ResponseHelper'")
  public void testGetMethodAndResponseHelper() {
    /* @Step 1: Get cat fact; Expected: Response */
    int statusCode = ResponseHelper.getStatusCode(MockApiHelper.getCatFactResponse());
    /* @Step 2: Verify status code; Expected: Status code is correct */
    MatcherAssert.assertThat("Status code is incorrect", statusCode, Matchers.is(HttpStatus.SC_OK));
  }

  @TmsLink("API_ID_2")
  @TestGroup("API")
  @Test(description = "Verify pojo 'Converter'")
  public void testPojoConverter() {
    /* @Step 1: Get cat fact pojo; Expected: Cat fact object */
    CatFactPojo catFactPojo = MockApiHelper.getCatFact();
    /* @Step 2: Verify that fact length from response as expected; Expected: Fact length is correct */
    MatcherAssert.assertThat("Fact length is incorrect", catFactPojo.fact().length(),
        Matchers.is(catFactPojo.length()));
  }

  @TmsLink("API_ID_3")
  @TestGroup("API")
  @Test(description = "Verify 'LocalDateFormatter' and 'LocalDateParser'")
  public void testLocalDateTimeFormatterAndParser() {
    SoftAssertions softAssertions = new SoftAssertions();

    /* @Step 1: Create user; Expected: user created */
    ReqresUserPojo.Response responsePojo = MockApiHelper.createUser();
    LocalDateTime expectedCreatedDate = LocalDateTimeGenerator.generateNow();
    LocalDateTime actualCreatedDate = LocalDateTimeParser.toLocalDateTime(responsePojo.createdAt(),
        DateFormat.YYYY_MM_DD_HH_MM_SS_MS_ISO);
    /* @Step 2: Verify LocalDateParser; Expected: created date is correct */
    softAssertions.assertThat(actualCreatedDate).as("Creation date is incorrect")
        .isCloseTo(expectedCreatedDate, Assertions.within(1, ChronoUnit.MINUTES));
    /* @Step 3: Verify LocalDateFormatter; Expected: created date as string is correct */
    softAssertions.assertThat(responsePojo.createdAt()).as("Creation date string is incorrect")
        .startsWith(LocalDateTimeFormatter.format(actualCreatedDate, DateFormat.YYYY_MM_DD));
    softAssertions.assertAll();
  }

  @TmsLink("API_ID_4")
  @TestGroup("API")
  @Test(description = "Verify scheme validation by Pojo class")
  public void testValidateSchemaByPojoClass() {
    /* @Step 1: Get cat fact; Expected: Response */
    ValidatableResponse validatableResponse = MockApiHelper.getCatFactResponse();
    /* @Step 2: Verify response schema by Pojo class; Expected: Schema should be correct */
    ResponseHelper.validateSchema(validatableResponse, CatFactPojo.class);
  }

  @TmsLink("API_ID_5")
  @TestGroup("API")
  @Test(description = "Verify scheme validation by json schema file")
  public void testValidateSchemaByJsonSchemaFile() {
    /* @Step 1: Get cat fact; Expected: Response */
    ValidatableResponse validatableResponse = MockApiHelper.getCatFactResponse();
    /* @Step 2: Verify response schema by json schema file; Expected: Schema should be correct */
    ResponseHelper.validateSchema(validatableResponse, "json_schemas/get-cat-fact.json");
  }

  @TmsLink("API_ID_6")
  @TestGroup("API")
  @Test(description = "Verify 'ApiVerification' steps")
  public void testApiVerificationSteps() {
    CatFactPojo catFactPojo = catFactApiSteps.getCatFact();
    catFactApiSteps.verify()
        .verifyGetFactText(catFactPojo);
  }
}
