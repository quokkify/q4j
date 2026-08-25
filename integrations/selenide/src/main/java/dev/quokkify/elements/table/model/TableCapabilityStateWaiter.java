package dev.quokkify.elements.table.model;

import java.time.Duration;
import java.util.Objects;

import dev.quokkify.util.Waiter;

import static com.codeborne.selenide.Configuration.pollingInterval;
import static com.codeborne.selenide.Configuration.timeout;

final class TableCapabilityStateWaiter {

  private TableCapabilityStateWaiter() {
  }

  static Duration defaultTimeout() {
    return Duration.ofMillis(timeout);
  }

  static Duration defaultPollingInterval() {
    return Duration.ofMillis(pollingInterval);
  }

  static void perform(String description, TableStateToken stateToken, Runnable action,
                      Duration timeout) {
    Objects.requireNonNull(description, "description");
    TableStateToken requiredToken = Objects.requireNonNull(stateToken, "stateToken");
    Runnable requiredAction = Objects.requireNonNull(action, "action");
    Duration requiredTimeout = Objects.requireNonNull(timeout, "timeout");
    String before = requiredToken.current();
    if (before == null) {
      throw new IllegalStateException("Table state token returned null before " + description);
    }
    requiredAction.run();
    Waiter.awaitCondition(() -> {
      String current = requiredToken.current();
      return current != null && !Objects.equals(current, before);
    },
        "Table state token did not change after " + description,
        requiredTimeout, defaultPollingInterval());
  }
}
