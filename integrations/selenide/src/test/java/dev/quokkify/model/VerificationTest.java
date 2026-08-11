package dev.quokkify.model;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import dev.quokkify.impl.Page;
import dev.quokkify.util.Waiter;

import org.assertj.core.api.Assertions;
import org.awaitility.core.ConditionTimeoutException;
import org.testng.annotations.Test;

public class VerificationTest {

  private static final class TestPage implements Page {
  }

  private static final class TestSteps extends PageSteps<TestSteps, TestVerification, TestPage> {
    TestSteps() {
      super.page = new TestPage();
      super.verification = new TestVerification(this, page);
    }
  }

  private static final class TestVerification extends Verification<TestSteps, TestVerification, TestPage> {
    TestVerification(TestSteps steps, TestPage page) {
      super(steps, page);
    }
  }

  @Test
  public void withTimeoutOverridesTheDefaultWaitDuration() {
    TestVerification verification = new TestSteps().verify().withTimeout(Duration.ofMillis(300));
    Instant start = Instant.now();

    Assertions.assertThatThrownBy(() -> Waiter.awaitAssertion(
            () -> Assertions.assertThat(true).isFalse(),
            verification.getTimeout(),
            verification.getPollingInterval()))
        .isInstanceOf(ConditionTimeoutException.class);

    Duration elapsed = Duration.between(start, Instant.now());
    Assertions.assertThat(elapsed).isBetween(Duration.ofMillis(300), Duration.ofSeconds(2));
  }

  @Test
  public void withPollingOverridesTheDefaultPollInterval() {
    AtomicInteger pollCount = new AtomicInteger();
    TestVerification verification = new TestSteps().verify()
        .withTimeout(Duration.ofSeconds(1))
        .withPolling(Duration.ofMillis(200));

    Runnable failingCheck = () -> {
      pollCount.incrementAndGet();
      Assertions.assertThat(true).isFalse();
    };
    Assertions.assertThatThrownBy(() -> Waiter.awaitAssertion(
            failingCheck::run,
            verification.getTimeout(),
            verification.getPollingInterval()))
        .isInstanceOf(ConditionTimeoutException.class);

    Assertions.assertThat(pollCount.get()).isBetween(3, 7);
  }
}
