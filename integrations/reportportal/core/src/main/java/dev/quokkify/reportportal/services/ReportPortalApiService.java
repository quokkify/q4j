package dev.quokkify.reportportal.services;

import java.time.Duration;

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

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

  private final ReportPortalFeignApi api;

  public ReportPortalApiService() {
    this(ReportPortalConfig.RP_ENDPOINT, ReportPortalConfig.RP_API_KEY);
  }

  ReportPortalApiService(String endpoint, String apiKey) {
    this(endpoint, apiKey, new Request.Options(CONNECT_TIMEOUT, READ_TIMEOUT, true));
  }

  ReportPortalApiService(String endpoint, String apiKey, Request.Options options) {
    this.api = Feign.builder()
        .client(new OkHttpClient())
        .encoder(new JacksonEncoder())
        .decoder(new JacksonDecoder())
        .options(options)
        .retryer(Retryer.NEVER_RETRY)
        .requestInterceptor(bearerAuthInterceptor(apiKey))
        .target(ReportPortalFeignApi.class, stripTrailingSlash(endpoint));
  }

  public ReportPortalItem getItemByUuid(String projectName, String itemUuid) {
    try {
      return api.getItemByUuid(projectName, itemUuid);
    } catch (FeignException.NotFound ignored) {
      return new ReportPortalItem(null, null, null);
    } catch (FeignException e) {
      throw new RuntimeException(
          "HTTP request failed: GET /api/v1/%s/item/uuid/%s".formatted(projectName, itemUuid), e);
    }
  }

  private static RequestInterceptor bearerAuthInterceptor(String apiKey) {
    return template -> template.header("Authorization", "Bearer " + apiKey);
  }

  private static String stripTrailingSlash(String endpoint) {
    return endpoint.replaceFirst("/+$", "");
  }
}
