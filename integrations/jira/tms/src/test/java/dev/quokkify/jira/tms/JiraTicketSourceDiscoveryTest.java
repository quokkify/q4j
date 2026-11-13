package dev.quokkify.jira.tms;

import java.util.ServiceLoader;

import dev.quokkify.tms.tickets.TicketSource;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraTicketSourceDiscoveryTest {

  @Test
  public void jiraTicketSourceIsDiscoveredViaServiceLoader() {
    assertThat(ServiceLoader.load(TicketSource.class))
        .anyMatch(JiraTicketSource.class::isInstance);
  }
}
