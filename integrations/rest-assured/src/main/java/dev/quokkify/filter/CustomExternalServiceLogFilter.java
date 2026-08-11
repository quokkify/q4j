package dev.quokkify.filter;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.http.HttpStatus;

/**
 * Filter for external services requests that should log only errors.
 */
public class CustomExternalServiceLogFilter extends ApiLogFilter implements Filter {

  @Override
  public Response filter(FilterableRequestSpecification requestSpec,
                         FilterableResponseSpecification responseSpec,
                         FilterContext context) {
    Response response = context.next(requestSpec, responseSpec);
    if (response.statusCode() >= HttpStatus.SC_BAD_REQUEST) {
      processFilter(requestSpec, response);
    }
    return response;
  }
}
