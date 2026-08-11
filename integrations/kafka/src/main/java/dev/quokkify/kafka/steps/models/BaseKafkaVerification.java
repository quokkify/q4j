package dev.quokkify.kafka.steps.models;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import dev.quokkify.util.Waiter;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseKafkaVerification<
        T extends BaseKafkaVerification<T, M>, M extends KafkaMessageValue>
    implements KafkaVerification {

  private static final Logger LOG = LogManager.getLogger(BaseKafkaVerification.class);

  private final Supplier<List<M>> messagesSupplier;
  private Duration timeout;
  private Duration pollingInterval;

  protected BaseKafkaVerification(Supplier<List<M>> messagesSupplier) {
    this(messagesSupplier, Duration.ofSeconds(30), Duration.ofMillis(1000));
  }

  protected BaseKafkaVerification(
      Supplier<List<M>> messagesSupplier, Duration timeout, Duration pollingInterval) {
    this.messagesSupplier = messagesSupplier;
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

  @Step("Kafka: contains message matching predicate")
  public T containsMessage(Predicate<M> predicate) {
    LOG.debug("Kafka verify: contains message matching predicate");
    Waiter.awaitCondition(
        () -> messagesSupplier.get().stream().anyMatch(predicate),
        "Expected Kafka message matching predicate",
        timeout,
        pollingInterval);
    return self();
  }

  @Step("Kafka: does not contain message matching predicate")
  public T doesNotContainMessage(Predicate<M> predicate) {
    LOG.debug("Kafka verify: does not contain message matching predicate");
    Waiter.assertNeverTrue(
        () -> messagesSupplier.get().stream().anyMatch(predicate),
        timeout,
        pollingInterval,
        "Unexpected Kafka message matching predicate");
    return self();
  }

  @Step("Kafka: has at least {count} messages matching predicate")
  public T hasMessageCount(int count, Predicate<M> predicate) {
    LOG.debug("Kafka verify: has at least {} messages matching predicate", count);
    Waiter.awaitCondition(
        () -> messagesSupplier.get().stream().filter(predicate).count() >= count,
        "Expected at least " + count + " Kafka messages matching predicate",
        timeout,
        pollingInterval);
    return self();
  }
}
