package dev.quokkify.test;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.filter.CustomExternalServiceLogFilter;

import io.qameta.allure.TmsLink;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CustomExternalServiceLogFilterTest {

  private FilterableRequestSpecification requestSpec;
  private FilterableResponseSpecification responseSpec;
  private FilterContext context;
  private Response mockResponse;
  private SpyableFilter filter;
  private AutoCloseable mocks;

  @BeforeMethod
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    requestSpec = mock(FilterableRequestSpecification.class);
    responseSpec = mock(FilterableResponseSpecification.class);
    context = mock(FilterContext.class);
    mockResponse = mock(Response.class);
    filter = new SpyableFilter();

    when(context.next(requestSpec, responseSpec)).thenReturn(mockResponse);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    mocks.close();
  }

  @DataProvider(name = "successStatusCodes")
  public Object[][] successStatusCodes() {
    return new Object[][] {{200}, {201}, {204}, {301}, {302}, {399}};
  }

  @DataProvider(name = "errorStatusCodes")
  public Object[][] errorStatusCodes() {
    return new Object[][] {{400}, {401}, {403}, {404}, {422}, {500}, {503}};
  }

  @SingleThread
  @TmsLink("FILTER_1")
  @Test(dataProvider = "successStatusCodes",
      description = "Filter does NOT log when response status is below 400")
  public void filter_withSuccessStatus_doesNotCallProcessFilter(int statusCode) {
    when(mockResponse.statusCode()).thenReturn(statusCode);

    filter.filter(requestSpec, responseSpec, context);

    assertThat(filter.processFilterCallCount)
        .as("processFilter must not be called for status %d", statusCode)
        .isZero();
  }

  @SingleThread
  @TmsLink("FILTER_2")
  @Test(dataProvider = "errorStatusCodes",
      description = "Filter logs request/response when status code is 400 or above")
  public void filter_withErrorStatus_callsProcessFilter(int statusCode) {
    when(mockResponse.statusCode()).thenReturn(statusCode);

    filter.filter(requestSpec, responseSpec, context);

    assertThat(filter.processFilterCallCount)
        .as("processFilter must be called exactly once for status %d", statusCode)
        .isEqualTo(1);
  }

  @SingleThread
  @TmsLink("FILTER_3")
  @Test(description = "Filter always returns the response from context.next regardless of status")
  public void filter_alwaysReturnsResponseFromContext() {
    when(mockResponse.statusCode()).thenReturn(200);
    Response result200 = filter.filter(requestSpec, responseSpec, context);
    assertThat(result200).as("2xx response must be returned as-is").isSameAs(mockResponse);

    when(mockResponse.statusCode()).thenReturn(500);
    Response result500 = filter.filter(requestSpec, responseSpec, context);
    assertThat(result500).as("5xx response must be returned as-is").isSameAs(mockResponse);
  }

  @SingleThread
  @TmsLink("FILTER_4")
  @Test(description = "Filter delegates execution to context.next exactly once per call")
  public void filter_delegatesToContextNextOnce() {
    when(mockResponse.statusCode()).thenReturn(200);

    filter.filter(requestSpec, responseSpec, context);

    verify(context, times(1)).next(requestSpec, responseSpec);
  }

  private static class SpyableFilter extends CustomExternalServiceLogFilter {

    int processFilterCallCount = 0;

    @Override
    protected Response processFilter(FilterableRequestSpecification requestSpec, Response response) {
      processFilterCallCount++;
      return response;
    }
  }
}
