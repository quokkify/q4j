package dev.quokkify.verification;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import dev.quokkify.util.Waiter;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseMongoVerification<T extends BaseMongoVerification<T>>
    implements MongoVerification {

  private static final Logger LOG = LogManager.getLogger(BaseMongoVerification.class);

  private Duration timeout;
  private Duration pollingInterval;

  protected BaseMongoVerification() {
    this(Duration.ofSeconds(30), Duration.ofMillis(1000));
  }

  protected BaseMongoVerification(Duration timeout, Duration pollingInterval) {
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

  @Step("MongoDB: has document matching predicate")
  public <E> T hasDocument(Callable<List<E>> query, Predicate<E> predicate) {
    LOG.debug("MongoDB verify: has document matching predicate");
    Waiter.awaitCondition(
        () -> query.call().stream().anyMatch(predicate),
        "Expected MongoDB document matching predicate",
        timeout,
        pollingInterval);
    return self();
  }

  @Step("MongoDB: does not have document matching predicate")
  public <E> T doesNotHaveDocument(Callable<List<E>> query, Predicate<E> predicate) {
    LOG.debug("MongoDB verify: does not have document matching predicate");
    Waiter.assertNeverTrue(
        () -> {
          try {
            return query.call().stream().anyMatch(predicate);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        },
        timeout,
        pollingInterval,
        "Unexpected MongoDB document matching predicate");
    return self();
  }

  @Step("MongoDB: document count is at least {count}")
  public <E> T hasDocumentCount(Callable<List<E>> query, int count) {
    LOG.debug("MongoDB verify: has at least {} documents", count);
    Waiter.awaitCondition(
        () -> query.call().size() >= count,
        "Expected at least " + count + " MongoDB documents",
        timeout,
        pollingInterval);
    return self();
  }
}
