package dev.quokkify.filter;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * Filter for tests requests that should log everything.
 */
public class CustomApiTestLogFilter extends ApiLogFilter implements Filter {

  @Override
  public Response filter(FilterableRequestSpecification requestSpec,
                         FilterableResponseSpecification responseSpec,
                         FilterContext context) {
    Response response = context.next(requestSpec, responseSpec);
    return processFilter(requestSpec, response);
  }
}
