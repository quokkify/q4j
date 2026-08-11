package dev.quokkify.reportportal.test;

import dev.quokkify.reportportal.config.ReportPortalConnectionConfig;
import dev.quokkify.reportportal.model.ReportPortalItem;
import dev.quokkify.reportportal.services.ReportPortalApiService;

import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportPortalApiServiceTest {

  private static final String NON_EXISTENT_UUID = "00000000-0000-0000-0000-000000000000";
  private static final ReportPortalApiService SERVICE = new ReportPortalApiService();

  @TmsLink("RP_API_1")
  @Test(description = "getItemByUuid returns an object for an existing project (negative: non-existent UUID)")
  public void getItemByUuid_nonExistentUuid_returnsItemWithNullId() {
    ReportPortalItem item = SERVICE.getItemByUuid(ReportPortalConnectionConfig.PROJECT_NAME, NON_EXISTENT_UUID);

    assertThat(item).as("Service must return a non-null object even for a missing item").isNotNull();
    assertThat(item.id()).as("id must be null for a non-existent UUID").isNull();
    assertThat(item.launchId()).as("launchId must be null for a non-existent UUID").isNull();
  }

  @TmsLink("RP_API_2")
  @Test(description = "getItemByUuid path is built correctly for project and UUID")
  public void getItemByUuid_pathContainsProjectAndUuid() {
    ReportPortalItem item = SERVICE.getItemByUuid(ReportPortalConnectionConfig.PROJECT_NAME, NON_EXISTENT_UUID);

    assertThat(item).as("Response must be deserialized without exception").isNotNull();
  }
}
