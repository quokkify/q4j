package dev.quokkify.tms.tickets;

import java.util.List;
import java.util.Map;

import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TicketCommentsTest {

  @TmsLink("TMS_TICKETS_1")
  @Test(description = "Tickets of all sources for one case are joined into one comment")
  public void joinsTicketsOfAllSources() {
    List<TicketSource> sources = List.of(
        new StubSource("Jira Issue(s)", Map.of("6", List.of("J-1"))),
        new StubSource("Issue(s)", Map.of("6", List.of("X-2"))));

    assertThat(TicketComments.byCaseId(sources))
        .containsExactly(Map.entry("6", "6 is disabled via **Jira Issue(s):** J-1; **Issue(s):** X-2"));
  }

  @TmsLink("TMS_TICKETS_2")
  @Test(description = "A source with an empty ticket list contributes nothing")
  public void emptyTicketListContributesNothing() {
    List<TicketSource> sources = List.of(new StubSource("Issue(s)", Map.of("7", List.of())));

    assertThat(TicketComments.byCaseId(sources)).isEmpty();
  }

  private record StubSource(String label, Map<String, List<String>> getTestCasesWithBugs) implements TicketSource {

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    public String buildTicketLink(String ticketId) {
      return ticketId;
    }
  }
}
