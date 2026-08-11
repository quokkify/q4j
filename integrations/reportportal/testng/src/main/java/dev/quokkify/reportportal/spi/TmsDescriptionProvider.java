package dev.quokkify.reportportal.spi;

public interface TmsDescriptionProvider {

  boolean isEnabled();

  String testCaseUrl(String tmsId);

  String enrichLaunchDescription();
}
