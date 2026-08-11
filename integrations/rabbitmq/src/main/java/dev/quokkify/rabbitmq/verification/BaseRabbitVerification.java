package dev.quokkify.rabbitmq.verification;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import dev.quokkify.rabbitmq.clients.RabbitClient;
import dev.quokkify.rabbitmq.clients.RabbitMessage;
import dev.quokkify.util.Waiter;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseRabbitVerification<T extends BaseRabbitVerification<T>>
    implements RabbitVerification {

  private static final Logger LOG = LogManager.getLogger(BaseRabbitVerification.class);

  private final Supplier<RabbitClient> clientSupplier;
  private Duration timeout;
  private Duration pollingInterval;

  protected BaseRabbitVerification(Supplier<RabbitClient> clientSupplier) {
    this(clientSupplier, Duration.ofSeconds(10), Duration.ofMillis(500));
  }

  protected BaseRabbitVerification(
      Supplier<RabbitClient> clientSupplier, Duration timeout, Duration pollingInterval) {
    this.clientSupplier = clientSupplier;
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

  @Step("RabbitMQ: queue '{queue}' has message")
  public T hasMessage(String queue) {
    LOG.debug("RabbitMQ verify: queue '{}' has message", queue);
    Waiter.awaitCondition(
        () -> clientSupplier.get().getMessage(queue, false).isPresent(),
        "Expected message in queue: " + queue,
        timeout,
        pollingInterval);
    return self();
  }

  @Step("RabbitMQ: queue '{queue}' has message matching predicate")
  public <B> T hasMessage(String queue, Predicate<RabbitMessage<B>> predicate) {
    LOG.debug("RabbitMQ verify: queue '{}' has message matching predicate", queue);
    Waiter.awaitCondition(
        () -> {
          Optional<RabbitMessage<B>> msg = clientSupplier.get().getMessage(queue, false);
          return msg.isPresent() && predicate.test(msg.get());
        },
        "Expected matching message in queue: " + queue,
        timeout,
        pollingInterval);
    return self();
  }

  @Step("RabbitMQ: queue '{queue}' has message with body containing '{substring}'")
  public T hasMessageWithBody(String queue, String substring) {
    LOG.debug(
        "RabbitMQ verify: queue '{}' has message with body containing '{}'", queue, substring);
    Waiter.awaitCondition(
        () -> {
          Optional<RabbitMessage<byte[]>> msg = clientSupplier.get().getMessage(queue, false);
          return msg.isPresent() && new String(msg.get().getPayload(), StandardCharsets.UTF_8).contains(substring);
        },
        "Expected message with body containing '" + substring + "' in queue: " + queue,
        timeout,
        pollingInterval);
    return self();
  }

  @Step("RabbitMQ: queue '{queue}' has no messages")
  public T doesNotHaveMessage(String queue) {
    LOG.debug("RabbitMQ verify: queue '{}' has no messages", queue);
    Waiter.assertNeverTrue(
        () -> clientSupplier.get().getMessage(queue, false).isPresent(),
        timeout,
        pollingInterval,
        "Unexpected message in queue: " + queue);
    return self();
  }
}
