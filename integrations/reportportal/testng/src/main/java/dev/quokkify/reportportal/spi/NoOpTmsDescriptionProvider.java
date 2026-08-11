package dev.quokkify.reportportal.spi;

import org.apache.commons.lang3.StringUtils;

public class NoOpTmsDescriptionProvider implements TmsDescriptionProvider {

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public String testCaseUrl(String tmsId) {
    return StringUtils.EMPTY;
  }

  @Override
  public String enrichLaunchDescription() {
    return StringUtils.EMPTY;
  }
}
