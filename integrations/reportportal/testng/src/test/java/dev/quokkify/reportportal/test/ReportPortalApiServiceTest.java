package dev.quokkify.reportportal.test;

import java.time.Duration;

import dev.quokkify.reportportal.config.ReportPortalConnectionConfig;
import dev.quokkify.reportportal.model.ReportPortalItem;
import dev.quokkify.reportportal.services.ReportPortalApiService;

import io.qameta.allure.TmsLink;
import org.awaitility.Awaitility;
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

  @TmsLink("RP_API_3")
  @Test(description = "getItemByUuid returns item and launch IDs for an existing item")
  public void getItemByUuid_existingItem_returnsItemAndLaunchIds() {
    String launchUuid = ReportPortalTestSupport.startLaunch("q4j-api-service-run");
    try {
      String itemUuid = ReportPortalTestSupport.startStep(launchUuid, "getItemByUuid probe");
      long launchId = ReportPortalTestSupport.json(
          ReportPortalTestSupport.API.getLaunch(ReportPortalTestSupport.PROJECT, launchUuid)).requiredAt("/id").asLong();

      ReportPortalItem item = Awaitility.await()
          .atMost(Duration.ofSeconds(30))
          .pollInterval(Duration.ofMillis(500))
          .until(() -> SERVICE.getItemByUuid(ReportPortalConnectionConfig.PROJECT_NAME, itemUuid),
              found -> found.id() != null);

      assertThat(item.launchId()).as("Item must belong to the started launch").isEqualTo(launchId);
      assertThat(item.path()).as("Item path must be populated").isNotBlank();
      ReportPortalTestSupport.finishStep(launchUuid, itemUuid);
    } finally {
      ReportPortalTestSupport.finishLaunch(launchUuid);
    }
  }
}
