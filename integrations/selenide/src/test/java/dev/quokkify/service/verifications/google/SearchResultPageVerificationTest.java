package dev.quokkify.service.verifications.google;

import java.time.Duration;
import java.time.Instant;

import dev.quokkify.page.google.SearchResultPage;
import dev.quokkify.service.steps.google.SearchResultPageSteps;

import org.assertj.core.api.Assertions;
import org.awaitility.core.ConditionTimeoutException;
import org.testng.annotations.Test;

public class SearchResultPageVerificationTest {

  private static final class EmptySearchResultPage extends SearchResultPage {
    @Override
    public int getSearchTitlesCount() {
      return 0;
    }
  }

  @Test
  public void verifySearchResultsExistHonoursCustomTimeoutAndPollingFromVerification() {
    SearchResultPageVerification verification = new SearchResultPageSteps(new EmptySearchResultPage()).verify()
        .withTimeout(Duration.ofMillis(500))
        .withPolling(Duration.ofMillis(100));
    Instant start = Instant.now();

    Assertions.assertThatThrownBy(verification::verifySearchResultsExist)
        .isInstanceOf(ConditionTimeoutException.class);

    Duration elapsed = Duration.between(start, Instant.now());
    Assertions.assertThat(elapsed).isBetween(Duration.ofMillis(500), Duration.ofSeconds(2));
  }
}
