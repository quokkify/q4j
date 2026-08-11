package dev.quokkify.tyrus.steps;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import dev.quokkify.tyrus.client.WsClient;
import dev.quokkify.tyrus.client.WsMessage;
import dev.quokkify.util.JsonConverter;
import dev.quokkify.util.Waiter;

import com.fasterxml.jackson.databind.JsonNode;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseWsVerification<T extends BaseWsVerification<T>> implements WsVerification {

  private static final Logger LOG = LogManager.getLogger(BaseWsVerification.class);

  protected final WsClient client;
  private Duration timeout;
  private Duration pollingInterval;

  protected BaseWsVerification(WsClient client) {
    this(client, Duration.ofSeconds(10), Duration.ofMillis(500));
  }

  protected BaseWsVerification(WsClient client, Duration timeout, Duration pollingInterval) {
    this.client = client;
    this.timeout = timeout;
    this.pollingInterval = pollingInterval;
  }

  protected abstract T self();

  public T withTimeout(Duration timeout) {
    this.timeout = timeout;
    return self();
  }

  public T withPolling(Duration pollingInterval) {
    this.pollingInterval = pollingInterval;
    return self();
  }

  @Step("WebSocket: contains message '{substring}'")
  public T containsMessage(String substring) {
    LOG.debug("WS verify: contains message '{}'", substring);
    Waiter.awaitCondition(
        () -> client.getMessages().stream().anyMatch(m -> m.payload().contains(substring)),
        "Expected WS message containing: " + substring,
        timeout, pollingInterval
    );
    return self();
  }

  @Step("WebSocket: contains message matching predicate")
  public T containsMessage(Predicate<WsMessage> predicate) {
    LOG.debug("WS verify: contains message matching predicate");
    Waiter.awaitCondition(
        () -> client.getMessages().stream().anyMatch(predicate),
        "Expected WS message matching predicate",
        timeout, pollingInterval
    );
    return self();
  }

  @Step("WebSocket: does not contain message '{substring}'")
  public T doesNotContainMessage(String substring) {
    LOG.debug("WS verify: does not contain message '{}'", substring);
    Waiter.assertNeverTrue(
        () -> client.getMessages().stream().anyMatch(m -> m.payload().contains(substring)),
        timeout, pollingInterval,
        "Unexpected WS message containing: " + substring
    );
    return self();
  }

  @Step("WebSocket: has JSON field '{field}' = '{expectedValue}'")
  public T hasJsonField(String field, String expectedValue) {
    LOG.debug("WS verify: has JSON field '{}' = '{}'", field, expectedValue);
    Waiter.awaitCondition(
        () -> client.getMessages().stream().anyMatch(m -> {
          try {
            JsonNode node = JsonConverter.fromString(m.payload(), JsonNode.class);
            String actual = node.path(field).asText(null);
            return expectedValue.equals(actual);
          } catch (Exception e) {
            return false;
          }
        }),
        "Expected WS message with JSON field '" + field + "' = '" + expectedValue + "'",
        timeout, pollingInterval
    );
    return self();
  }

  @Step("WebSocket: has at least {expected} messages")
  public T hasMessageCount(int expected) {
    LOG.debug("WS verify: has at least {} messages", expected);
    Waiter.awaitCondition(
        () -> client.getMessages().size() >= expected,
        "Expected at least " + expected + " WS messages",
        timeout, pollingInterval
    );
    return self();
  }

  @Step("WebSocket: messages in order")
  public T messagesInOrder(String... substrings) {
    LOG.debug("WS verify: messages in order {}", Arrays.toString(substrings));
    Waiter.awaitCondition(
        () -> {
          List<WsMessage> messages = client.getMessages();
          int idx = 0;
          for (WsMessage msg : messages) {
            if (idx < substrings.length && msg.payload().contains(substrings[idx])) {
              idx++;
            }
          }
          return idx == substrings.length;
        },
        "Expected WS messages in order: " + Arrays.toString(substrings),
        timeout, pollingInterval
    );
    return self();
  }
}
