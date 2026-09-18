package dev.quokkify.reportportal.services;

import dev.quokkify.reportportal.model.ReportPortalItem;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

interface ReportPortalFeignApi {

  @RequestLine("GET /api/v1/{projectName}/item/uuid/{itemUuid}")
  @Headers("Accept: application/json")
  ReportPortalItem getItemByUuid(
      @Param("projectName") String projectName,
      @Param("itemUuid") String itemUuid);
}
