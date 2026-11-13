package dev.quokkify.tms.tickets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.quokkify.constant.StringConstant;

/**
 * Builds comments for test cases that are excluded because of open tickets.
 */
public final class TicketComments {

  private TicketComments() {
  }

  /**
   * @return enabled ticket sources found by {@link ServiceLoader}
   */
  public static List<TicketSource> load() {
    return StreamSupport.stream(ServiceLoader.load(TicketSource.class).spliterator(), false)
        .filter(TicketSource::isEnabled)
        .toList();
  }

  /**
   * @param sources ticket sources
   * @return case id -> comment, for example {@code 6 is disabled via **Jira Issue(s):** J-1; **Issue(s):** X-2}
   */
  public static Map<String, String> byCaseId(List<TicketSource> sources) {
    Map<String, List<String>> segmentsByCaseId = new LinkedHashMap<>();
    sources.forEach(source -> source.getTestCasesWithBugs().forEach((caseId, tickets) -> {
      if (Objects.isNull(tickets) || tickets.isEmpty()) {
        return;
      }
      String links = tickets.stream()
          .map(source::buildTicketLink)
          .collect(Collectors.joining(StringConstant.COMMA_SPACE));
      segmentsByCaseId.computeIfAbsent(caseId, key -> new ArrayList<>())
          .add("**%s:** %s".formatted(source.label(), links));
    }));

    Map<String, String> commentsByCaseId = new LinkedHashMap<>();
    segmentsByCaseId.forEach((caseId, segments) -> commentsByCaseId.put(caseId,
        "%s is disabled via %s".formatted(caseId, String.join(StringConstant.SEMICOLON_SPACE, segments))));
    return commentsByCaseId;
  }
}
