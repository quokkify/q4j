package dev.quokkify.testrail.tickets;

import java.util.List;
import java.util.Map;

/**
 * Source of ticket references for test cases.
 */
public interface TicketSource {

  /**
   * @return true if source is configured and should be used.
   */
  boolean isEnabled();

  /**
   * @return map: testCaseId -> ticket ids
   */
  Map<String, List<String>> getTestCasesWithBugs();

  /**
   * @param ticketId ticket id
   * @return formatted link text for comment
   */
  String buildTicketLink(String ticketId);

  /**
   * @return label for the ticket source (e.g., "Jira Issue(s)")
   */
  default String label() {
    return "Issue(s)";
  }
}
