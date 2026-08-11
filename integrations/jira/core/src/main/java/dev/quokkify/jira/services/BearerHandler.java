package dev.quokkify.jira.services;

import com.atlassian.httpclient.api.Request;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;

/**
 * Handler for authorization in Jira with a token.
 */
class BearerHandler implements AuthenticationHandler {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private final String token;

  BearerHandler(String token) {
    this.token = token;
  }

  @Override
  public void configure(Request.Builder builder) {
    builder.setHeader(AUTHORIZATION_HEADER, "Bearer " + token);
  }
}
