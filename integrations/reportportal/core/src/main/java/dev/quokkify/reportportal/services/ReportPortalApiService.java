package dev.quokkify.reportportal.services;

import dev.quokkify.reportportal.configs.ReportPortalConfig;
import dev.quokkify.reportportal.model.ReportPortalItem;

import feign.Feign;
import feign.FeignException;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;

public class ReportPortalApiService {

  private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
  private static final int READ_TIMEOUT_MILLIS = 30_000;

  private final ReportPortalFeignApi api;

  public ReportPortalApiService() {
    this(ReportPortalConfig.RP_ENDPOINT, ReportPortalConfig.RP_API_KEY);
  }

  ReportPortalApiService(String endpoint, String apiKey) {
    this.api = Feign.builder()
        .client(new OkHttpClient())
        .encoder(new JacksonEncoder())
        .decoder(new JacksonDecoder())
        .options(new Request.Options(CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS))
        .retryer(Retryer.NEVER_RETRY)
        .requestInterceptor(bearerAuthInterceptor(apiKey))
        .target(ReportPortalFeignApi.class, stripTrailingSlash(endpoint));
  }

  public ReportPortalItem getItemByUuid(String projectName, String itemUuid) {
    try {
      return api.getItemByUuid(projectName, itemUuid);
    } catch (FeignException e) {
      throw new RuntimeException(
          "HTTP request failed: GET /api/v1/%s/item/uuid/%s".formatted(projectName, itemUuid), e);
    }
  }

  private static RequestInterceptor bearerAuthInterceptor(String apiKey) {
    return template -> {
      template.header("Authorization", "Bearer " + apiKey);
      template.header("Content-Type", "application/json");
      template.header("Accept", "application/json");
    };
  }

  private static String stripTrailingSlash(String endpoint) {
    return endpoint.replaceFirst("/+$", "");
  }
}
