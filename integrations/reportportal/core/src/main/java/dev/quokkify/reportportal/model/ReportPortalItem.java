package dev.quokkify.reportportal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportPortalItem(Long id, Long launchId, String path) {
}
